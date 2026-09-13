package com.thindie.rknzbl.appversion

import com.thindie.rknzbl.appfeatures.home.data.ProfileHttpGateway
import com.v2ray.ang.extension.runSuspendCatching
import com.v2ray.ang.runtime.KeyValueStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.days

/**
 * Resolves the version published on the WebDAV [versionUrl] and exposes it as a flow so screens can
 * react when a newer build is available.
 *
 * The result is cached in storage with a one-week TTL: each launch re-checks only once the previous
 * fetch has expired, so the source is hit at most once a week. Resolution failures resolve to
 * "no update shown" rather than surfacing an error.
 */
interface AppVersionResolver {
  /** Latest resolved remote version, or null until the first successful resolve. */
  val remoteVersion: StateFlow<AppVersion?>

  /** Fetches and parses the remote version line. Any failure resolves to null (no update shown). */
  suspend fun resolveRemote(): Result<AppVersion?>

  /** Runs [resolveRemote] once on [scope] and publishes the result into [remoteVersion]. */
  fun start(scope: CoroutineScope)

  /** True when a strictly newer remote version is available than the local build. */
  fun isUpdateAvailable(remote: AppVersion?): Boolean
}

class AppVersionResolverImpl(
  private val gateway: ProfileHttpGateway,
  private val versionUrl: String,
  private val localVersion: AppVersion,
  private val storage: KeyValueStorage,
) : AppVersionResolver {
  private val _remoteVersion = MutableStateFlow<AppVersion?>(null)
  override val remoteVersion: StateFlow<AppVersion?> = _remoteVersion.asStateFlow()

  override suspend fun resolveRemote(): Result<AppVersion?> =
    runSuspendCatching(
      block = {
        val version = cachedOrFetch()
        if (version != null) Result.success(version) else Result.success(null)
      },
      onError = { Result.success(null) },
    )

  /** Returns the cached remote version while it is fresh, otherwise fetches and stores it. */
  private suspend fun cachedOrFetch(): AppVersion? {
    val fetchedAtMs = storage.decodeRemoteVersionFetchedAtMs()
    if (isCacheFresh(fetchedAtMs)) {
      AppVersion.parse(storage.decodeRemoteVersion())?.let { return it }
    }
    return fetchAndStore()
  }

  private suspend fun fetchAndStore(): AppVersion? {
    val raw = gateway.fetchAuthenticated(versionUrl)
    val version = AppVersion.parse(raw)
    if (version != null) storage.storeRemoteVersion(raw, System.currentTimeMillis())
    return version
  }

  /** A cached version is valid for [VERSION_CACHE_TTL] after its fetch timestamp. */
  private fun isCacheFresh(fetchedAtMs: Long): Boolean =
    fetchedAtMs > 0L && System.currentTimeMillis() - fetchedAtMs < VERSION_CACHE_TTL.inWholeMilliseconds

  override fun start(scope: CoroutineScope) {
    scope.launch {
      when (val remote = resolveRemote().getOrNull()) {
        null -> Unit
        else -> _remoteVersion.value = remote
      }
    }
  }

  override fun isUpdateAvailable(remote: AppVersion?): Boolean = remote != null && remote > localVersion
}

/** How long a fetched remote version stays valid before the next network check is allowed. */
private val VERSION_CACHE_TTL = 7.days
