package com.thindie.rknzbl.appfeatures.home.data

import com.thindie.engine.core.Log
import com.thindie.rknzbl.error.AppError
import com.v2ray.ang.AppConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BasicAuthCredentials
import io.ktor.client.plugins.auth.providers.basic
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.http.withCharset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds

/**
 * Thin HTTP layer for profile sources: WebDAV storage (basic auth) and plain subscription fetch.
 *
 * Keeps all Ktor clients and request plumbing out of [ConnectionProfileRepositoryImpl] so the
 * repository can be unit-tested with a fake gateway.
 */
interface ProfileHttpGateway {
  /** Reads the stored profiles body from this gateway's WebDAV endpoint. */
  suspend fun readWebDav(): String

  /** Replaces the stored profiles body on this gateway's WebDAV endpoint. */
  suspend fun writeWebDav(body: String)

  /** Applies new WebDAV endpoint/credentials so the cached client is rebuilt on next use. */
  fun updateWebDav(
    webDavUrl: String,
    userName: String,
    password: String,
  )

  fun resetWebDav()

  suspend fun fetchSource(url: String): String

  /** Fetches an arbitrary file from the default WebDAV endpoint using its stored credentials. */
  suspend fun fetchAuthenticated(url: String): String
}

class ProfileHttpGatewayImpl(
  private val webDavUrlDefault: String,
  private val userNameDefault: String,
  private val passwordDefault: String,
) : ProfileHttpGateway {
  // Rebuilt when the WebDAV endpoint/credentials change (in-app settings update), so a running app
  // picks up new values without a restart. Double-checked under [clientLock]; the field is volatile
  // so readers observe the latest reference without taking the lock on every request.
  @Volatile
  private var webDavClient: HttpClient? = null
  private val clientLock = Any()
  private val params: MutableStateFlow<Triple<String, String, String>?> = MutableStateFlow(null)

  private val webDavUrl get() = requireNotNull(params.value?.first)
  private val userName get() = requireNotNull(params.value?.second)
  private val password get() = requireNotNull(params.value?.third)

  /** Applies new WebDAV endpoint/credentials and forces the authenticated client to be rebuilt. */
  override fun updateWebDav(
    webDavUrl: String,
    userName: String,
    password: String,
  ) {
    params.update {
      Triple(webDavUrl, userName, password)
    }
    webDavClient = null
  }

  override fun resetWebDav() {
    updateWebDav(
      webDavUrl = webDavUrlDefault,
      userName = userNameDefault,
      password = passwordDefault,
    )
  }

  private fun davClient(): HttpClient =
    webDavClient ?: synchronized(clientLock) {
      webDavClient
        ?: newAuthenticatedWebdavClient(userName, password)
          .also { webDavClient = it }
    }

  private val httpClient: HttpClient by lazy {
    HttpClient(CIO) {
      engine {
        maxConnectionsCount = MAX_CONNECTIONS_COUNT
      }
      install(HttpTimeout) {
        requestTimeoutMillis = REQUEST_TIMEOUT_MS
        connectTimeoutMillis = CONNECT_TIMEOUT_MS
        socketTimeoutMillis = SOCKET_TIMEOUT_MS
      }
    }
  }

  // Dedicated authenticated client for fixed deploy-time endpoints (e.g. version.txt). Uses the
  // default WebDAV credentials only, so it is unaffected by any user-configured endpoint change.
  private val defaultAuthClient: HttpClient by lazy { newAuthenticatedWebdavClient(userNameDefault, passwordDefault) }

  override suspend fun readWebDav(): String =
    withTimeoutOrNull(REQUEST_TIMEOUT) {
      try {
        val client = davClient()
        Log.d({ "WebDAV GET: configure client $webDavUrl}" }, LOG_TAG)
        val response = client.get(webDavUrl)
        Log.d({ "WebDAV GET: status=$response }" }, LOG_TAG)
        if (!response.status.isSuccess()) {
          throw webDavErrorFromStatus(response.status, webDavUrl)
        }
        val result = response.body<String>().trim()
        Log.d({ "WebDAV GET: status=${response.status}, body length=${result.length}" }, LOG_TAG)
        result
      } catch (e: CancellationException) {
        throw e
      } catch (_: HttpRequestTimeoutException) {
        throw AppError.ServerError.TimeOut
      } catch (_: IOException) {
        throw AppError.WebDav.UploadOpenFailed
      }
    } ?: throw AppError.ServerError.TimeOut

  override suspend fun fetchSource(url: String): String =
    withTimeoutOrNull(REQUEST_TIMEOUT) {
      try {
        val response = httpClient.get(url)

        val result = response.body<String>().trim()
        Log.d({ "Remote GET: status=${response.status}, body length=${result.length}" }, LOG_TAG)
        result
      } catch (e: CancellationException) {
        throw e
      } catch (_: HttpRequestTimeoutException) {
        throw AppError.ServerError.TimeOut
      } catch (_: IOException) {
        throw AppError.WebDav.UploadOpenFailed
      }
    } ?: throw AppError.ServerError.TimeOut

  override suspend fun fetchAuthenticated(url: String): String =
    withTimeoutOrNull(REQUEST_TIMEOUT) {
      try {
        val response = defaultAuthClient.get(url)
        if (!response.status.isSuccess()) {
          throw webDavErrorFromStatus(response.status, url)
        }
        val result = response.body<String>().trim()
        Log.d({ "Version GET: status=${response.status}, body length=${result.length}" }, LOG_TAG)
        result
      } catch (e: CancellationException) {
        throw e
      } catch (_: HttpRequestTimeoutException) {
        throw AppError.ServerError.TimeOut
      } catch (_: IOException) {
        throw AppError.WebDav.UploadOpenFailed
      }
    } ?: throw AppError.ServerError.TimeOut

  override suspend fun writeWebDav(body: String) {
    withTimeoutOrNull(REQUEST_TIMEOUT) {
      try {
        val response =
          davClient().put(webDavUrl) {
            contentType(ContentType.Text.Plain.withCharset(Charsets.UTF_8))
            setBody(body)
          }
        if (!response.status.isSuccess()) {
          throw webDavErrorFromStatus(response.status, webDavUrl)
        }
      } catch (e: CancellationException) {
        throw e
      } catch (_: HttpRequestTimeoutException) {
        throw AppError.ServerError.TimeOut
      } catch (_: IOException) {
        throw AppError.ServerError.ConnectionFailed
      }
    } ?: throw AppError.ServerError.TimeOut
  }

  private fun newAuthenticatedWebdavClient(
    userName: String,
    password: String,
  ): HttpClient =
    HttpClient(CIO) {
      engine {
        maxConnectionsCount = MAX_CONNECTIONS_COUNT
      }
      install(HttpTimeout) {
        requestTimeoutMillis = REQUEST_TIMEOUT_MS
        connectTimeoutMillis = CONNECT_TIMEOUT_MS
        socketTimeoutMillis = SOCKET_TIMEOUT_MS
      }
      install(Auth) {
        basic {
          credentials {
            BasicAuthCredentials(username = userName, password = password)
          }
          sendWithoutRequest { true }
        }
      }
    }
}

private fun webDavErrorFromStatus(
  status: HttpStatusCode,
  requestedUrl: String? = null,
): AppError.WebDav =
  when (status) {
    HttpStatusCode.Unauthorized -> AppError.WebDav.Unauthorized
    HttpStatusCode.Forbidden -> AppError.WebDav.Forbidden
    HttpStatusCode.NotFound -> AppError.WebDav.NotFound(requestedUrl = requestedUrl)
    HttpStatusCode.Conflict -> AppError.WebDav.Conflict
    HttpStatusCode.MethodNotAllowed -> AppError.WebDav.Conflict
    else -> AppError.WebDav.InvalidPropfindResponse
  }

private const val MAX_CONNECTIONS_COUNT = 32
private const val REQUEST_TIMEOUT_MS = 300_000L
private const val CONNECT_TIMEOUT_MS = 30_000L
private const val SOCKET_TIMEOUT_MS = 300_000L
private val REQUEST_TIMEOUT = 5.seconds

private val LOG_TAG = AppConfig.TAG
