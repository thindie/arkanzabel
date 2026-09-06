package com.thindie.rknzbl.feature.home.data

import android.content.Context
import com.thindie.engine.core.Cache
import com.thindie.engine.core.Log
import com.thindie.rknzbl.application.ProfilePingManager
import com.thindie.rknzbl.error.AppError
import com.thindie.rknzbl.feature.home.domain.ConnectionProfileRepository
import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.ConnectionProfile
import com.v2ray.ang.enums.Protocol
import com.v2ray.ang.runtime.KeyValueStorage
import com.v2ray.ang.runtime.ProfileUriParser
import com.v2ray.ang.runtime.V2RayServiceManager
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.cancellation.CancellationException

class ConnectionProfileRepositoryImpl(
  private val appContext: Context,
  private val pingManager: ProfilePingManager,
  private val userName: String,
  private val password: String,
  private val url: String,
  private val storage: KeyValueStorage,
) : ConnectionProfileRepository {
  private val httpClient: HttpClient by lazy {
    newAuthenticatedWebdavClient(userName, password)
  }

  private val isLocalSave get() = storage.isLocalSaveEnabled()

  // Separate caches: stored (local) vs remote sources
  private val storedProfilesCache = Cache<List<ConnectionProfile>>(null)
  private val remoteSourceCaches = mutableMapOf<String, Cache<List<ConnectionProfile>>>()
  private val activeProfileCache = Cache<ConnectionProfile>(null)

  private val autoSavedEvents =
    MutableSharedFlow<String>(
      replay = 0,
      extraBufferCapacity = 3,
      onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

  // Reactive API state backed by Cache<T>
  private val profilesCacheReactive = Cache<List<ConnectionProfile>>(emptyList())
  override val profiles: Flow<List<ConnectionProfile>> = profilesCacheReactive.value.map { it ?: emptyList() }

  override val stored: Flow<List<ConnectionProfile>> = profilesCacheReactive.value.map { it ?: emptyList() }

  private val measuredCache = Cache<ConnectionProfile?>(null)
  override val lastMeasured: Flow<ConnectionProfile?> = measuredCache.value

  // Reactive view of the currently selected profile; updated on connect and cleared on disconnect.
  override val connected: Flow<ConnectionProfile?> = activeProfileCache.value

  override suspend fun read(): List<ConnectionProfile> {
    if (isLocalSave) {
      val cached = storedProfilesCache.get()
      if (cached != null) return cached

      val body = storage.getLocalProfiles().orEmpty()
      val profiles = parseAndDeduplicate(body)
      storedProfilesCache.set(profiles)
      return profiles
    } else {
      return readFromSource(url)
    }
  }

  override suspend fun save(guid: String): Boolean {
    if (guid.isBlank()) return false
    val profilePretty = storage.decodeServerConfig(guid)
    if (profilePretty == null) {
      Log.w({ "Save Profile: failure, decodeServerConfig" }, LOG_TAG)
      return false
    }
    if (isLocalSave) {
      val currentBody = storage.getLocalProfiles().orEmpty()
      val isSaved =
        if (currentBody.isEmpty()) {
          false
        } else {
          isSavedInternal(
            connectionProfile = profilePretty,
            currentBody = currentBody,
          )
        }
      if (isSaved) return false
      val profileJson = JsonUtil.toJson(profilePretty)
      val updatedBody = currentBody + SEPARATOR + profileJson
      storage.setLocalProfiles(updatedBody)
      Log.i({ "Save Profile [LOCAL]: success" }, LOG_TAG)
      storedProfilesCache.clear()
      profilesCacheReactive.clear()
      return true
    } else {
      val currentBody = readInternal(httpClient, url)
      val isSaved = isSavedInternal(profilePretty, currentBody)
      if (isSaved) return false
      val profileJson = JsonUtil.toJson(profilePretty)
      writeInternal(httpClient, url, currentBody + SEPARATOR + profileJson)
      Log.i({ "Save Profile: success" }, LOG_TAG)
      remoteSourceCaches[url]?.clear()
      profilesCacheReactive.clear()
      return true
    }
  }

  override suspend fun delete(profile: ConnectionProfile) {
    if (!isLocalSave) {
      val profileJson = JsonUtil.toJson(profile)
      val client = httpClient
      val currentBody = readInternal(client, url)
      val filteredBody = currentBody.replace(oldValue = profileJson, "")
      val fallbackBody = filteredBody.replace(oldValue = SEPARATOR + SEPARATOR, SEPARATOR)
      writeInternal(client, url, fallbackBody)
      remoteSourceCaches[url]?.clear()
      profilesCacheReactive.clear()
    } else {
      val profileJson = JsonUtil.toJson(profile)
      val currentBody = storage.getLocalProfiles().orEmpty()
      val filteredBody = currentBody.replace(oldValue = profileJson, "")
      val fallbackBody = filteredBody.replace(oldValue = SEPARATOR + SEPARATOR, SEPARATOR)
      storage.setLocalProfiles(fallbackBody)
      storedProfilesCache.clear()
      profilesCacheReactive.clear()
    }
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
    val cached = activeProfileCache.get()
    if (cached != null) return cached
    val profile = activeProfileInternal()
    if (profile != null) activeProfileCache.set(profile)
    return profile
  }

  override fun isSaved(profile: ConnectionProfile): Boolean {
    val storedCached = storedProfilesCache.get()
    if (storedCached?.firstOrNull { it.subscriptionId == profile.subscriptionId } != null) return true

    // Check remote caches too
    for ((_, cache) in remoteSourceCaches) {
      if (cache.get()?.firstOrNull { it.subscriptionId == profile.subscriptionId } != null) {
        return true
      }
    }
    return false
  }

  override fun invalidateCaches() {
    invalidateCacheInternal()
  }

  // VPN service operations
  override suspend fun connect(profile: ConnectionProfile) {
    val guid = findOrSaveProfileGuid(profile)
    if (guid == null) {
      throw IllegalStateException("Cannot store profile: ${profile.subscriptionId}")
    }
    activeProfileCache.set(profile)
    V2RayServiceManager.startVService(context = appContext, guid = guid)
  }

  override suspend fun disconnect() {
    activeProfileCache.clear()
    V2RayServiceManager.stopVService(appContext)
  }

  override fun isConnected(): Boolean {
    return V2RayServiceManager.isRunning()
  }

  override fun getConnectedServerName(): String {
    return V2RayServiceManager.getRunningServerName()
  }

  private val fetching = AtomicBoolean(false)

  override suspend fun fetch() {
    if (!fetching.compareAndSet(false, true)) return
    try {
      Log.d({ "Fetch profiles: start" }, LOG_TAG)
      // Step 1: Go to the network with whichever source is currently selected in settings.
      // Redesigned design: a non-blank URL means the custom source is active.
      val customUrl = storage.getCustomSourceUrl()
      val loadedProfiles =
        if (!customUrl.isNullOrBlank()) {
          Log.d({ "Fetch profiles: using custom source" }, LOG_TAG)
          readFromSource(customUrl)
        } else {
          Log.d({ "Fetch profiles: using default source" }, LOG_TAG)
          read()
        }
      Log.d({ "Fetch profiles: loaded ${loadedProfiles.size} profiles" }, LOG_TAG)
      profilesCacheReactive.set(loadedProfiles)

      if (loadedProfiles.isEmpty()) {
        Log.w({ "Fetch profiles: empty list from source, aborting measurement" }, LOG_TAG)
        measuredCache.clear()
        return
      }

      // Check if any profiles are pingable before measuring (Custom/PolicyGroup cannot be pinged)
      val hasPingable =
        loadedProfiles.any {
          it.protocol != Protocol.Custom && it.protocol != Protocol.PolicyGroup
        }
      if (!hasPingable) {
        Log.d({ "Fetch profiles: no pingable profiles (Custom/PolicyGroup only), skipping measurement" }, LOG_TAG)
        measuredCache.clear()
        return
      }

      // Step 2: Measure all profiles and find the best one
      Log.d({ "Fetch profiles: measuring ${loadedProfiles.size} profiles" }, LOG_TAG)
      val batchId = pingManager.pingProfiles(loadedProfiles, force = false)

      // Wait for this specific batch's result with a timeout to prevent hanging.
      // Per-profile timeouts inside the manager keep the total bounded as well.
      val resultsMap =
        if (batchId < 0) {
          null
        } else {
          withTimeoutOrNull(30_000L) {
            pingManager.batch.first { it?.id == batchId }
              ?.results
          }
        }

      if (resultsMap.isNullOrEmpty()) {
        Log.w({ "Fetch profiles: measurement results not received within 30s" }, LOG_TAG)
        measuredCache.clear()
        return
      }
      Log.d({ "Fetch profiles: got ${resultsMap.size} measurement results" }, LOG_TAG)

      // Select best profile (lowest latency); -1 marks an unreachable profile and must not win.
      val bestProfile =
        loadedProfiles
          .mapNotNull { profile -> resultsMap[profile]?.takeIf { it >= 0 }?.let { profile to it } }
          .minByOrNull { (_, delay) -> delay }
          ?.first

      if (bestProfile != null) {
        Log.i({ "Fetch profiles: best profile ${bestProfile.subscriptionId}" }, LOG_TAG)
        measuredCache.set(bestProfile)
      } else {
        Log.w({ "Fetch profiles: no reachable profile found" }, LOG_TAG)
        measuredCache.clear()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: AppError) {
      // Expected server/network errors from read()
      Log.e({ "Failed to fetch and measure profiles" }, LOG_TAG, e)
      measuredCache.clear()
    } finally {
      fetching.set(false)
    }
  }

  private fun isSavedInternal(
    connectionProfile: ConnectionProfile,
    currentBody: String,
  ): Boolean {
    val id = connectionProfile.subscriptionId
    Log.v({ "Save Profile: check for $id" }, LOG_TAG)
    if (id in currentBody) {
      Log.d({ "Save Profile: already saved" }, LOG_TAG)
      return true
    }
    return false
  }

  /**
   * Returns the GUID of [profile] if it is already stored locally (matched by subscriptionId),
   * otherwise persists it and returns the new GUID. Null when profile cannot be stored.
   */
  private suspend fun findOrSaveProfileGuid(profile: ConnectionProfile): String? {
    for (guid in storage.decodeServerList()) {
      val config = storage.decodeServerConfig(guid)
      if (config != null && config.subscriptionId == profile.subscriptionId) {
        return guid
      }
    }
    val guid = UUID.randomUUID().toString()
    storage.encodeServerConfig(guid, profile)
    Log.i({ "Connect: profile stored locally as $guid" }, LOG_TAG)
    return guid
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

  override fun invalidateStoredCache() {
    storedProfilesCache.clear()
  }

  override suspend fun invalidateRemoteCache(url: String) {
    remoteSourceCaches[url]?.clear()
  }

  override suspend fun readFromSource(sourceUrl: String): List<ConnectionProfile> {
    val cache =
      remoteSourceCaches.getOrPut(sourceUrl) { Cache<List<ConnectionProfile>>(null) }

    val cached = cache.get()
    if (cached != null) {
      Log.d({ "Read from source: cache hit (${cached.size} profiles)" }, LOG_TAG)
      return cached
    }

    Log.d({ "Read from source: fetching from network" }, LOG_TAG)
    val body = readInternal(httpClient, sourceUrl)
    val profiles = parseAndDeduplicate(body)
    Log.d({ "Read from source: parsed ${profiles.size} profiles" }, LOG_TAG)
    cache.set(profiles)
    return profiles
  }

  private fun invalidateCacheInternal() {
    storedProfilesCache.clear()
    remoteSourceCaches.values.forEach { it.clear() }
    activeProfileCache.clear()
    profilesCacheReactive.clear()
    measuredCache.clear()
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

private suspend fun readFromUrl(
  client: HttpClient,
  url: String,
): List<ConnectionProfile> {
  val body = readInternal(client, url)
  return parseAndDeduplicate(body)
}

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

private val LOG_TAG = AppConfig.TAG

private fun parseAndDeduplicate(body: String): List<ConnectionProfile> {
  return parseRemote(body)
    .flatMap { parseChunk(it) }
    .toSet()
    .toList()
}

/**
 * Lenient chunk parsing: a malformed chunk is logged and skipped instead of failing the whole fetch.
 *
 * Supports two formats:
 * - JSON chunks (local-save format, `########`-separated)
 * - subscription share lines (one URI per line, `#` lines are comments/headers)
 */
private fun parseChunk(chunk: String): List<ConnectionProfile> {
  if (chunk.startsWith("{")) {
    return listOfNotNull(JsonUtil.fromJsonOrNull(chunk, ConnectionProfile::class.java))
  }
  return chunk.lineSequence()
    .map { it.trim() }
    .filter { it.isNotEmpty() && !it.startsWith("#") }
    .mapNotNull { ProfileUriParser.parse(it) }
    .toList()
}

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
