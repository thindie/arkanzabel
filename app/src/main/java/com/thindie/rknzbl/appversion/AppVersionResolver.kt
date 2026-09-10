package com.thindie.rknzbl.appversion

import com.thindie.rknzbl.appfeatures.home.data.ProfileHttpGateway
import com.v2ray.ang.extension.runSuspendCatching
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Resolves the version published on the remote source and exposes it as a flow so screens can react
 * when a newer build is available.
 *
 * The remote URL is intentionally abstract for now — swap [REMOTE_VERSION_URL] for the real endpoint
 * once it lands. Resolution failures resolve to "no update shown" rather than surfacing an error.
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
) : AppVersionResolver {
  private val _remoteVersion = MutableStateFlow<AppVersion?>(null)
  override val remoteVersion: StateFlow<AppVersion?> = _remoteVersion.asStateFlow()

  override suspend fun resolveRemote(): Result<AppVersion?> =
    runSuspendCatching(
      block = {
        val version = AppVersion.parse(gateway.fetchSource(versionUrl))
        if (version != null) Result.success(version) else Result.success(null)
      },
      onError = { Result.success(null) },
    )

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

/** Placeholder remote source — read the first line as a semantic version. Replace with the real URL. */
const val REMOTE_VERSION_URL = "https://example.com/app/version.txt"
