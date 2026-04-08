package com.thindie.rknzbl.feature.home.data

import com.thindie.rknzbl.error.AppError
import com.thindie.rknzbl.feature.home.domain.ConnectionProfileRepository
import com.v2ray.ang.dto.ConnectionProfile
import com.v2ray.ang.runtime.KeyValueStorage
import com.v2ray.ang.util.JsonUtil
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
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

class ConnectionProfileRepositoryImpl(
  private val userName: String,
  private val password: String,
  private val url: String,
  private val storage: KeyValueStorage,
) : ConnectionProfileRepository {

  private val httpClient: HttpClient by lazy {
    newAuthenticatedWebdavClient(userName, password)
  }

  override suspend fun read(): List<ConnectionProfile> {
    val client = httpClient
    val body = readInternal(client, url)

    return parseRemote(body)
      .mapNotNull {
        JsonUtil.fromJson(it, ConnectionProfile::class.java)
      }
      .toSet()
      .toList()
  }

  override suspend fun save(guid: String) {
    if (guid.isBlank()) return
    val profilePretty = storage.decodeServerConfig(guid)
    if (profilePretty == null) return
    val client = httpClient
    val profileJson = JsonUtil.toJson(profilePretty)
    val currentBody = readInternal(client, url)
    writeInternal(client, url, currentBody + SEPARATOR + profileJson)
  }
}

private fun parseRemote(trimmedBody: String): List<String> {
  if (trimmedBody.isEmpty()) return emptyList()
  return trimmedBody
    .split(SEPARATOR)
    .map { it.trim() }
    .filter { it.isNotEmpty() }
}

private suspend fun readInternal(client: HttpClient, url: String): String =
  withTimeoutOrNull(5_000L) {
    try {
      val response = client.get(url)
      if (!response.status.isSuccess()) {
        throw webDavErrorFromStatus(response.status, url)
      }
      response.body<String>().trim()
    } catch (e: CancellationException) {
      throw e
    } catch (_: HttpRequestTimeoutException) {
      throw AppError.ServerError.TimeOut
    } catch (_: IOException) {
      throw AppError.WebDav.UploadOpenFailed
    }
  } ?: throw AppError.ServerError.TimeOut

private suspend fun writeInternal(client: HttpClient, url: String, body: String) {
  withTimeoutOrNull(5_000L) {
    try {
      val response = client.put(url) {
        contentType(ContentType.Text.Plain.withCharset(Charsets.UTF_8))
        setBody(body)
      }
      if (!response.status.isSuccess()) {
        throw webDavErrorFromStatus(response.status, url)
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

private const val SEPARATOR = "########"

private fun newAuthenticatedWebdavClient(userName: String, password: String): HttpClient =
  HttpClient(CIO) {
    engine {
      maxConnectionsCount = 32
    }
    install(HttpTimeout) {
      requestTimeoutMillis = 300_000
      connectTimeoutMillis = 30_000
      socketTimeoutMillis = 300_000
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

internal fun webDavErrorFromStatus(
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
