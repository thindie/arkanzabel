package com.thindie.rknzbl.feature.home.data

import android.util.Log
import com.thindie.rknzbl.error.AppError
import com.thindie.rknzbl.feature.home.domain.ConnectionProfileRepository
import com.v2ray.ang.AppConfig
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
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.updateAndGet
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

  private val profilesCache: MutableStateFlow<List<ConnectionProfile>?> = MutableStateFlow(null)

  private val autoSavedEvents =
    MutableSharedFlow<String>(
      replay = 0,
      extraBufferCapacity = 3,
      onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

  override suspend fun read(): List<ConnectionProfile> {
    val cache = profilesCache.value
    if (cache != null) return cache

    val client = httpClient
    val body = readInternal(client, url)

    val profiles =
      parseRemote(body)
        .mapNotNull {
          JsonUtil.fromJson(it, ConnectionProfile::class.java)
        }
        .toSet()
        .toList()

    profilesCache.updateAndGet { profiles }
    return requireNotNull(profilesCache.value)
  }

  override suspend fun save(guid: String): Boolean {
    if (guid.isBlank()) return false
    val profilePretty = storage.decodeServerConfig(guid)
    if (profilePretty == null) {
      Log.i(AppConfig.TAG, "Save Profile: failure, decodeServerConfig")
      return false
    }
    val (currentBody, isSaved) = isSavedInternal(profilePretty)
    if (isSaved) return false
    val profileJson = JsonUtil.toJson(profilePretty)
    writeInternal(httpClient, url, currentBody + SEPARATOR + profileJson)
    Log.i(AppConfig.TAG, "Save Profile: success")
    invalidateCache()
    return true
  }

  override suspend fun delete(profile: ConnectionProfile) {
    val profileJson = JsonUtil.toJson(profile)
    val client = httpClient
    val currentBody = readInternal(client, url)
    val filteredBody = currentBody.replace(oldValue = profileJson, "")
    val fallbackBody = filteredBody.replace(oldValue = SEPARATOR + SEPARATOR, SEPARATOR)
    writeInternal(client, url, fallbackBody)
    invalidateCache()
  }

  override fun autoSaved(): Flow<ConnectionProfile?> {
    return autoSavedEvents
      .map { guid ->
        storage.decodeServerConfig(guid)
      }
  }

  override suspend fun fetchAutoSaved() {
    val guid = KeyValueStorage.getLastAutoSaveProfilesJson()
    guid?.let { autoSavedEvents.tryEmit(it) }
  }

  override suspend fun activeProfile(): ConnectionProfile? {
    return activeProfileInternal()
  }

  override fun isSaved(profile: ConnectionProfile): Boolean {
    val profilesCache = profilesCache.value
    return if (profilesCache != null) {
      profilesCache.firstOrNull { it.subscriptionId == profile.subscriptionId } != null
    } else {
      false
    }
  }

  private suspend fun isSavedInternal(connectionProfile: ConnectionProfile): Pair<String, Boolean> {
    val id = connectionProfile.subscriptionId
    Log.i(AppConfig.TAG, "Save Profile: check for $id")
    val client = httpClient
    val currentBody = readInternal(client, url)
    if (id in currentBody) {
      Log.i(AppConfig.TAG, "Save Profile: already saved")
      return currentBody to true
    }
    return currentBody to false
  }

  private fun activeProfileInternal(): ConnectionProfile? {
    val guid = storage.getSelectServer() ?: return null
    val profilePretty = storage.decodeServerConfig(guid)
    return profilePretty
  }

  override suspend fun saveAuto(guid: String) {
    KeyValueStorage.setLastAutoSaveProfilesJson(guid)
    autoSavedEvents.tryEmit(guid)
  }

  override suspend fun markAutoSavedSeen() {
    KeyValueStorage.setLastAutoSaveProfilesJson("")
    autoSavedEvents.tryEmit("")
  }

  private fun invalidateCache() {
    profilesCache.value = null
  }
}

private fun parseRemote(trimmedBody: String): List<String> {
  if (trimmedBody.isEmpty()) return emptyList()
  return trimmedBody
    .split(SEPARATOR)
    .map { it.trim() }
    .filter { it.isNotEmpty() }
}

private suspend fun readInternal(
  client: HttpClient,
  url: String,
): String =
  withTimeoutOrNull(5_000L) {
    try {
      val response = client.get(url)
      if (!response.status.isSuccess()) {
        throw webDavErrorFromStatus(response.status, url)
      }
      val result = response.body<String>().trim()
      Log.i("readInternal: ", result)
      result
    } catch (e: CancellationException) {
      throw e
    } catch (_: HttpRequestTimeoutException) {
      throw AppError.ServerError.TimeOut
    } catch (_: IOException) {
      throw AppError.WebDav.UploadOpenFailed
    }
  } ?: throw AppError.ServerError.TimeOut

private suspend fun writeInternal(
  client: HttpClient,
  url: String,
  body: String,
) {
  withTimeoutOrNull(5_000L) {
    try {
      val response =
        client.put(url) {
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

private fun newAuthenticatedWebdavClient(
  userName: String,
  password: String,
): HttpClient =
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
