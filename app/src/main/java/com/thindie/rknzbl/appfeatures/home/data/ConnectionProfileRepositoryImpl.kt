package com.thindie.rknzbl.appfeatures.home.data

import com.thindie.engine.core.Cache
import com.thindie.engine.core.Log
import com.thindie.engine.core.WorkState
import com.thindie.rknzbl.domain.ConnectionProfileRepository
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull

class ConnectionProfileRepositoryImpl(
  private val storage: KeyValueStorage,
  private val httpGateway: ProfileHttpGateway,
  private val vpnGateway: VpnServiceGateway,
) : ConnectionProfileRepository {
  private val isLocalSave get() = storage.isLocalSaveEnabled()
  private val localStorageKey get() = if (isLocalSave) "prefs_stored" else "webdav_stored"
  private val cacheLock: Any = Any()
  private val profilesInMemoryCache = mutableMapOf<String, Cache<List<ConnectionProfile>?>>()

  // Bumped on every [profilesInMemoryCache] mutation so the derived flows re-read current values.
  private val cacheVersion = MutableStateFlow(0)

  private fun cacheValueSyncInternal(k: String): List<ConnectionProfile>? {
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

  private val activeProfileCache = Cache<ConnectionProfile?>(null)

  // Emits null while the VPN service reports an error, so a failed start does not look connected.
  override val connected: Flow<ConnectionProfile?> =
    combine(activeProfileCache.value, vpnGateway.serviceState) { profile, state ->
      if (state is WorkState.Error) null else profile
    }

  override val measured: Flow<List<ConnectionProfile>?> =
    cacheVersion.map {
      cacheValueSyncInternal(MEASURED_PROFILES_CACHE_KEY)
    }

  override suspend fun ping() {
    pingInternal()
  }

  private suspend fun pingInternal(cap: Int? = null): List<ConnectionProfile> {
    var measured = emptyList<ConnectionProfile>()
    profilesInMemoryCache
      .filter { it.key != MEASURED_PROFILES_CACHE_KEY && it.key != SKIPPED_PROFILES_CACHE_KEY }
      .filter { it.key == storage.getCustomSourceUrl() }
      .forEach { (_, cache) ->
        val profiles = cache.value.value ?: return@forEach
        if (!ensureVpnRunning(profiles)) return@forEach
        val resultProfiles = profiles.toMutableList()
        var capInternal = 0
        profiles.forEachIndexed { index, profile ->
          if (capInternal == cap) {
            return@forEachIndexed
          }
          val measuredProfile = httpGateway.ping(profile)
          if (measuredProfile.delay != null && measuredProfile.delay!! > 0) {
            capInternal += 1
          }
          resultProfiles[index] = measuredProfile
          setCacheInternal(MEASURED_PROFILES_CACHE_KEY, resultProfiles.toList())
        }
        measured = resultProfiles
      }
    return measured
  }

  override suspend fun asapProfile() {
    val cachedMeasured = cacheValueSyncInternal(MEASURED_PROFILES_CACHE_KEY)?.ifEmpty { null }
    if (cachedMeasured != null) {
      val active = activeProfileCache.value.value
      val skipped = cacheValueSyncInternal(SKIPPED_PROFILES_CACHE_KEY)
      val updatedSkipped = (skipped.orEmpty() + active).filterNotNull()
      val skippedIds =
        updatedSkipped
          .map { it.subscriptionId }
      val candidates =
        cachedMeasured
          .filterNot { it.subscriptionId in skippedIds || it.subscriptionId == active?.subscriptionId }
      // Only reachable profiles (delay > 0) are eligible; minByOrNull would otherwise
      // prefer an unreachable one (-1) over a working server with positive latency.
      val reachable = candidates.filter { it.delay?.let { d -> d > 0 } == true }
      if (reachable.isEmpty()) {
        invalidateCacheInternal()
        asapProfile()
      } else {
        val best = reachable.minByOrNull { it.delay ?: Long.MAX_VALUE }
        setCacheInternal(SKIPPED_PROFILES_CACHE_KEY, updatedSkipped)
        connect(requireNotNull(best))
      }
    } else {
      val url = storage.getCustomSourceUrl() ?: return
      val profiles = fetchFromSource(url)
      if (profiles.isEmpty() || !ensureVpnRunning(profiles)) return
      val measured = pingInternal(NEXT_BEST_PING_CAP)
      val best =
        measured.filter { it.delay?.let { d -> d > 0 } == true }
          .minByOrNull { it.delay ?: Long.MAX_VALUE }
      setCacheInternal(MEASURED_PROFILES_CACHE_KEY, measured)
      best?.let { connect(it) }
    }
  }

  private suspend fun ensureVpnRunning(profiles: List<ConnectionProfile>): Boolean {
    if (vpnGateway.isRunning()) return true
    val bootstrapProfile = profiles.firstOrNull() ?: return false
    warmupWithAnyProfile(bootstrapProfile)
    return withTimeoutOrNull(VPN_START_TIMEOUT_MS) {
      vpnGateway.serviceState.first { it is WorkState.Running }
    } != null
  }

  init {
    // Restore active profile from storage if VPN service is already running.
    // This handles the case where the activity was killed but the process survived
    // due to the background VPN service, so reopening the app shows the correct state.
    if (vpnGateway.isRunning()) {
      val restored = activeProfileInternal()
      if (restored != null) {
        activeProfileCache.set(restored)
      }
    }
  }

  // Profiles received from remote sources: view of the active source URL's cache entry.
  override val received: Flow<List<ConnectionProfile>?> =
    cacheVersion.map { storage.getCustomSourceUrl()?.let { url -> cacheValueSyncInternal(url) } }

  // Stored (saved) profiles: view of the local-storage key's cache entry.
  override val stored: Flow<List<ConnectionProfile>?> =
    cacheVersion.map { storageCacheInternal() }

  override suspend fun read(): List<ConnectionProfile> {
    val cached = storageCacheInternal()
    if (cached != null) return cached

    // Stored profiles live in local prefs or on WebDAV depending on the active storage mode.
    val body =
      if (isLocalSave) {
        storage.getLocalProfiles().orEmpty()
      } else {
        httpGateway.readWebDav()
      }
    Log.d({ "Read stored: body length=${body.length}" }, LOG_TAG)
    val profiles = parseAndDeduplicate(body, STORED_PROFILES_SEPARATOR)
    Log.d({ "Read stored: parsed ${profiles.size} profiles" }, LOG_TAG)
    setStorageCacheInternal(profiles)
    return profiles
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

  override fun invalidateCaches() {
    invalidateCacheInternal()
  }

  // VPN service operations
  override suspend fun connect(profile: ConnectionProfile) {
    activeProfileCache.set(profile)
    vpnGateway.startVService(profile)
  }

  private fun warmupWithAnyProfile(profile: ConnectionProfile) {
    vpnGateway.startVService(profile)
  }

  override suspend fun disconnect() {
    activeProfileCache.clear()
    vpnGateway.stopVService()
  }

  override val vpnState: Flow<WorkState> = vpnGateway.serviceState

  override suspend fun fetch(force: Boolean) {
    val url = storage.getCustomSourceUrl()
    url?.let { url ->
      if (force) invalidateInternal(url)
      if (cacheValueSyncInternal(url) != null) return
      fetchFromSource(url)
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

  private fun activeProfileInternal(): ConnectionProfile? {
    val guid = storage.getSelectServer() ?: return null
    val profilePretty = storage.decodeServerConfig(guid)
    return profilePretty
  }

  override suspend fun saveAuto(guid: String) {
    KeyValueStorage.setLastAutoSaveProfilesJson(guid)
    autoSavedEvents.tryEmit(guid)
  }

  override fun invalidateStoredCache() {
    invalidateStoredInternal()
  }

  override fun isLocalStorage(): Boolean = storage.isLocalSaveEnabled()

  private suspend fun fetchFromSource(url: String): List<ConnectionProfile> {
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

private const val SKIPPED_PROFILES_CACHE_KEY = "skipped_measured_cache"
private const val MEASURED_PROFILES_CACHE_KEY = "measured_profiles_measured_cache"

private const val NEXT_BEST_PING_CAP = 5
private const val VPN_START_TIMEOUT_MS = 30_000L
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
