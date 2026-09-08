package com.thindie.rknzbl.feature.home.data

import com.thindie.engine.core.Cache
import com.thindie.engine.core.Log
import com.thindie.engine.core.WorkState
import com.thindie.rknzbl.application.ProfilePingManager
import com.thindie.rknzbl.feature.home.domain.ConnectionProfileRepository
import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.ConnectionProfile
import com.v2ray.ang.runtime.KeyValueStorage
import com.v2ray.ang.runtime.ProfileUriParser
import com.v2ray.ang.util.JsonUtil
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

class ConnectionProfileRepositoryImpl(
  private val pingManager: ProfilePingManager,
  private val storage: KeyValueStorage,
  private val httpGateway: ProfileHttpGateway,
  private val vpnGateway: VpnServiceGateway,
) : ConnectionProfileRepository {
  private val isLocalSave get() = storage.isLocalSaveEnabled()

  // Getter, not a constructor-time val: the mode can be switched at runtime and the cache key must follow.
  private val localStorageKey get() = if (isLocalSave) "prefs_stored" else "webdav_stored"
  private val cacheLock: Any = Any()
  private val profilesInMemoryCache = mutableMapOf<String, Cache<List<ConnectionProfile>?>>()

  // Bumped on every [profilesInMemoryCache] mutation so the derived flows re-read current values.
  private val cacheVersion = MutableStateFlow(0)

  private fun cacheValueSyncInternal(k: String): List<ConnectionProfile>? {
    // Read under the same lock as mutations to avoid racing a structural map update.
    return synchronized(cacheLock) { profilesInMemoryCache[k]?.value?.value }
  }

  private fun setCacheInternal(
    k: String,
    v: List<ConnectionProfile>,
  ) {
    synchronized(cacheLock) {
      val current = profilesInMemoryCache[k]
      if (current != null) {
        current.updateValue { v }
      } else {
        profilesInMemoryCache[k] = Cache(v)
      }
    }
    cacheVersion.update { it + 1 }
  }

  private fun invalidateInternal(k: String) {
    synchronized(cacheLock) {
      val current = profilesInMemoryCache[k]
      current?.clear()
    }
    cacheVersion.update { it + 1 }
  }

  private fun invalidateStoredInternal() = invalidateInternal(localStorageKey)

  private fun storageCacheInternal(): List<ConnectionProfile>? = cacheValueSyncInternal(localStorageKey)

  private fun setStorageCacheInternal(profiles: List<ConnectionProfile>) {
    setCacheInternal(localStorageKey, profiles)
  }

  /** Re-parses [body] and publishes it to the stored caches (map + reactive flow). */
  private fun refreshStoredCacheInternal(body: String) {
    invalidateStoredInternal()
    setStorageCacheInternal(parseAndDeduplicate(body, STORED_PROFILES_SEPARATOR))
  }

  private val autoSavedEvents =
    MutableSharedFlow<String>(
      replay = 0,
      extraBufferCapacity = 3,
      onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

  // Best profile of the last completed ping batch (lowest successful delay);
  // null until a batch yields at least one successful measurement.
  override val lastMeasured: Flow<ConnectionProfile?> =
    pingManager.batch.map { batch ->
      batch?.results
        ?.filterValues { it > 0 }
        ?.minByOrNull { it.value }
        ?.key
    }

  // Reactive view of the currently selected profile; updated on connect and cleared on disconnect.
  private val activeProfileCache = Cache<ConnectionProfile?>(null)

  // Emits null while the VPN service reports an error, so a failed start does not look connected.
  override val connected: Flow<ConnectionProfile?> =
    combine(activeProfileCache.value, vpnGateway.serviceState) { profile, state ->
      if (state is WorkState.Error) null else profile
    }

  // Pending connect intent: set by [requestConnect], consumed when the reactive path launches a
  // connect, cleared on disconnect. Intentionally survives route recreation so a pending intent
  // completes once the user returns to Home.
  private val connectRequested = AtomicBoolean(false)

  override fun requestConnect() {
    connectRequested.set(true)
  }

  override fun takeConnectIntent(): Boolean = connectRequested.getAndSet(false)

  // Profiles received from remote sources: view of the active source URL's cache entry.
  override val received: Flow<List<ConnectionProfile>?> =
    cacheVersion.map { storage.getCustomSourceUrl()?.let { url -> cacheValueSyncInternal(url) } }

  // Stored (saved) profiles: view of the local-storage key's cache entry.
  override val stored: Flow<List<ConnectionProfile>?> =
    cacheVersion.map { storageCacheInternal() }

  override suspend fun read(): List<ConnectionProfile> {
    if (isLocalSave) {
      val cached = storageCacheInternal()
      if (cached != null) return cached

      val body = storage.getLocalProfiles().orEmpty()
      val profiles = parseAndDeduplicate(body, STORED_PROFILES_SEPARATOR)
      setStorageCacheInternal(profiles)
      return profiles
    } else {
      val sourceUrl = storage.getCustomSourceUrl() ?: return emptyList()
      return fetchFromSource(sourceUrl)
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
      val updatedBody = currentBody + STORED_PROFILES_SEPARATOR + profileJson
      storage.setLocalProfiles(updatedBody)
      Log.i({ "Save Profile [LOCAL]: success" }, LOG_TAG)
      refreshStoredCacheInternal(updatedBody)
      return true
    } else {
      val currentBody = httpGateway.readWebDav()
      val isSaved = isSavedInternal(profilePretty, currentBody)
      if (isSaved) return false
      val profileJson = JsonUtil.toJson(profilePretty)
      val updatedBody = currentBody + STORED_PROFILES_SEPARATOR + profileJson
      httpGateway.writeWebDav(updatedBody)
      Log.i({ "Save Profile: success" }, LOG_TAG)
      refreshStoredCacheInternal(updatedBody)
      return true
    }
  }

  override suspend fun delete(profile: ConnectionProfile) {
    if (!isLocalSave) {
      val profileJson = JsonUtil.toJson(profile)
      val currentBody = httpGateway.readWebDav()
      val filteredBody = currentBody.replace(oldValue = profileJson, "")
      val fallbackBody = filteredBody.replace(oldValue = STORED_PROFILES_SEPARATOR + STORED_PROFILES_SEPARATOR, STORED_PROFILES_SEPARATOR)
      httpGateway.writeWebDav(fallbackBody)
      refreshStoredCacheInternal(fallbackBody)
    } else {
      val profileJson = JsonUtil.toJson(profile)
      val currentBody = storage.getLocalProfiles().orEmpty()
      val filteredBody = currentBody.replace(oldValue = profileJson, "")
      val fallbackBody = filteredBody.replace(oldValue = STORED_PROFILES_SEPARATOR + STORED_PROFILES_SEPARATOR, STORED_PROFILES_SEPARATOR)
      storage.setLocalProfiles(fallbackBody)
      refreshStoredCacheInternal(fallbackBody)
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
    val storedCached = storageCacheInternal()
    return (storedCached?.firstOrNull { it.subscriptionId == profile.subscriptionId } != null)
  }

  override fun invalidateCaches() {
    invalidateCacheInternal()
  }

  // VPN service operations
  override suspend fun connect(profile: ConnectionProfile) {
    val guid =
      findOrSaveProfileGuid(profile)
        ?: error("Cannot store profile: ${profile.subscriptionId}")
    activeProfileCache.set(profile)
    vpnGateway.startVService(guid = guid)
  }

  override suspend fun disconnect() {
    connectRequested.set(false)
    activeProfileCache.clear()
    vpnGateway.stopVService()
  }

  override fun isConnected(): Boolean {
    return vpnGateway.isRunning()
  }

  override fun getConnectedServerName(): String {
    return vpnGateway.getRunningServerName()
  }

  override val vpnState: Flow<WorkState> = vpnGateway.serviceState

  override suspend fun fetch(force: Boolean) {
    val url = storage.getCustomSourceUrl()
    url?.let { url ->
      if (force) invalidateInternal(url)
      if (cacheValueSyncInternal(url) != null) return
      fetchFromSource(url)
      val profiles = requireNotNull(cacheValueSyncInternal(url))
      pingManager.pingProfiles(profiles, force = false)
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
    invalidateStoredInternal()
  }

  override suspend fun invalidateRemoteCache(url: String) {
    invalidateInternal(url)
  }

  override suspend fun fetchFromSource(url: String): List<ConnectionProfile> {
    val cached = cacheValueSyncInternal(url)
    if (cached != null) {
      Log.d({ "Read from source: cache hit (${cached.size} profiles)" }, LOG_TAG)
      return cached
    }

    Log.d({ "Read from source: fetching from network" }, LOG_TAG)
    val body = httpGateway.fetchSource(url)
    val profiles = parseAndDeduplicate(body, FETCH_PROFILES_SEPARATOR)
    Log.d({ "Read from source: parsed ${profiles.size} profiles" }, LOG_TAG)
    setCacheInternal(url, profiles)
    return requireNotNull(cacheValueSyncInternal(url))
  }

  private fun invalidateCacheInternal() {
    synchronized(cacheLock) {
      profilesInMemoryCache.clear()
    }
    cacheVersion.update { it + 1 }
    activeProfileCache.clear()
  }
}

private fun parseRemote(
  trimmedBody: String,
  delimeter: String,
): List<String> {
  if (trimmedBody.isEmpty()) return emptyList()
  return trimmedBody
    .split(delimeter)
    .map { it.trim() }
    .filter { it.isNotEmpty() }
}

private const val STORED_PROFILES_SEPARATOR = "########"
private const val FETCH_PROFILES_SEPARATOR = "\n"

private val LOG_TAG = AppConfig.TAG

private fun parseAndDeduplicate(
  body: String,
  delimeter: String,
): List<ConnectionProfile> {
  return parseRemote(trimmedBody = body, delimeter = delimeter)
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
