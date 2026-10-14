package com.thindie.rknzbl.appfeatures.settings.data

import com.thindie.engine.uikit.ThemeSwitcher
import com.thindie.rknzbl.appfeatures.home.data.ProfileHttpGateway
import com.thindie.rknzbl.appfeatures.settings.data.theme.toChoice
import com.thindie.rknzbl.appfeatures.settings.data.theme.toStorageString
import com.thindie.rknzbl.domain.SettingsRepository
import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.WebDavConfig
import com.v2ray.ang.runtime.KeyValueStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull

/**
 * [SettingsRepository] backed by [KeyValueStorage].
 *
 * Every option is a [Setting]: a private [MutableStateFlow] seeded from storage, exposed to the UI
 * as a read-only [Flow]. A change is persisted immediately by [Setting.set] / [Setting.toggle],
 * which fixes two latent bugs of the previous inline pattern — a toggle was lost when no subscriber
 * was attached, and string setters could persist without updating the flow (or vice versa).
 */
class SettingsRepositoryImpl(
  private val storage: KeyValueStorage,
  private val profileHttpGateway: ProfileHttpGateway,
) : SettingsRepository {
  // --- Theme mode ---
  // Storage returns null until the user has ever touched settings; default to "auto" so callers get
  // an immediate non-null value instead of a flow that never emits.
  private val themeSetting =
    Setting<ThemeSwitcher.Choice>(
      read = { storage.getThemeMode()?.toChoice() ?: ThemeSwitcher.Choice.Auto },
      write = { storage.setThemeMode(it.toStorageString()) },
    )
  override val themeChoice: Flow<ThemeSwitcher.Choice> get() = themeSetting.flow

  // --- Autosave ---
  private val autosaveSetting =
    Setting<Boolean>(
      read = { storage.isAutosaveEnabled() },
      write = { storage.encodeSettings(AppConfig.PREF_AUTO_SAVE_ACTIVE_PROFILE_ENABLED, it) },
    )
  override val autosaveEnabled: Flow<Boolean> get() = autosaveSetting.flow

  override suspend fun toggleAutosave(enabled: Boolean) {
    autosaveSetting.set(enabled)
  }

  // --- MUX support ---
  private val muxSetting =
    Setting<Boolean>(
      read = { storage.decodeSettingsBool(AppConfig.PREF_MUX_ENABLED, false) },
      write = { storage.encodeSettings(AppConfig.PREF_MUX_ENABLED, it) },
    )
  override val muxEnabled: Flow<Boolean> get() = muxSetting.flow

  override suspend fun toggleMux(enabled: Boolean) {
    muxSetting.set(enabled)
  }

  // --- Local storage mode support ---
  private val localSaveSetting =
    Setting<Boolean>(
      read = { storage.isLocalSaveEnabled() },
      write = { storage.setLocalSaveMode(it) },
    )
  override val isLocalSave: Flow<Boolean> get() = localSaveSetting.flow

  override suspend fun toggleLocalSave(enabled: Boolean) {
    localSaveSetting.set(enabled)
  }

  // --- Speed notification support ---
  private val speedSetting =
    Setting<Boolean>(
      read = { storage.decodeSettingsBool(AppConfig.PREF_SPEED_ENABLED, false) },
      write = { storage.encodeSettings(AppConfig.PREF_SPEED_ENABLED, it) },
    )
  override val speedEnabled: Flow<Boolean?> get() = speedSetting.flow

  override suspend fun toggleSpeed(enabled: Boolean) {
    speedSetting.set(enabled)
  }

  // --- Language ---
  private val languageSetting =
    Setting<String>(
      read = { storage.decodeSettingsString(AppConfig.PREF_LANGUAGE).orEmpty() },
      write = { storage.encodeSettings(AppConfig.PREF_LANGUAGE, it) },
    )
  override val language: Flow<String> get() = languageSetting.flow.filterNotNull()

  override fun setLanguage(code: String) {
    languageSetting.set(code)
  }

  // --- Custom source URL support (redesigned design: presence of a non-blank URL means active) ---
  private val customSourceUrlSetting =
    Setting<String?>(
      read = { storage.getCustomSourceUrl() },
      write = { storage.setCustomSourceUrl(it) },
    )
  override val customSourceUrl: Flow<String?> get() = customSourceUrlSetting.flow

  /** Persists [url]; null/blank clears the key and disables the custom source. */
  override fun setCustomSourceUrl(url: String?) {
    customSourceUrlSetting.set(url)
  }

  // --- WebDAV storage config (URL / login / password) ---
  private val webDavConfigState = MutableStateFlow(storage.decodeWebDavConfig())
  override val webDavConfig: Flow<WebDavConfig?> get() = webDavConfigState

  /** Persists [config]; null clears the key and disables remote storage. */
  override fun setWebDavConfig(config: WebDavConfig?) {
    webDavConfigState.value = config
    if (config == null) {
      storage.clearWebDavConfig()
      profileHttpGateway.resetWebDav()
    } else {
      profileHttpGateway.updateWebDav(
        config.baseUrl,
        config.username.orEmpty(),
        config.password.orEmpty(),
      )
      storage.encodeWebDavConfig(config)
    }
  }

  // --- WebDAV: use built-in default config instead of manual entry (default off) ---
  private val webDavUseDefaultsSetting =
    Setting<Boolean>(
      read = { storage.decodeSettingsBool(AppConfig.PREF_WEBDAV_USE_DEFAULTS, true) },
      write = {
        if (it) {
          profileHttpGateway.resetWebDav()
        }
        storage.encodeSettings(AppConfig.PREF_WEBDAV_USE_DEFAULTS, it)
      },
    )
  override val webDavUseDefaults: Flow<Boolean> get() = webDavUseDefaultsSetting.flow

  override suspend fun toggleWebDavUseDefaults(enabled: Boolean) {
    webDavUseDefaultsSetting.set(enabled)
  }

  // --- Parallel ping count (profile measurement concurrency) ---
  private val parallelPingSetting =
    Setting<Int>(
      read = { storage.decodeSettingsInt(AppConfig.PREF_PARALLEL_PING_COUNT, AppConfig.DEFAULT_PARALLEL_PING_COUNT) },
      write = { storage.encodeSettings(AppConfig.PREF_PARALLEL_PING_COUNT, it) },
    )
  override val parallelPingCount: Flow<Int> get() = parallelPingSetting.flow

  override fun setParallelPingCount(count: Int) {
    parallelPingSetting.set(clampParallelPing(count))
  }

  // --- ASAP series size (reachable profiles measured per cycle) ---
  private val asapSeriesSizeSetting =
    Setting<Int>(
      read = { storage.decodeSettingsInt(AppConfig.PREF_ASAP_SERIES_SIZE, AppConfig.DEFAULT_ASAP_SERIES_SIZE) },
      write = { storage.encodeSettings(AppConfig.PREF_ASAP_SERIES_SIZE, it) },
    )
  override val asapSeriesSize: Flow<Int> get() = asapSeriesSizeSetting.flow

  override fun setAsapSeriesSize(size: Int) {
    asapSeriesSizeSetting.set(clampAsapSeries(size))
  }

  // Keep the parallel-ping worker count within a sane range (>=1, capped so it cannot spawn more
  // ephemeral cores than there are profiles to measure).
  private fun clampParallelPing(count: Int): Int = count.coerceIn(AppConfig.MIN_PARALLEL_PING_COUNT, AppConfig.MAX_PARALLEL_PING_COUNT)

  // Keep the ASAP series length within a sane range.
  private fun clampAsapSeries(size: Int): Int = size.coerceIn(AppConfig.MIN_ASAP_SERIES_SIZE, AppConfig.MAX_ASAP_SERIES_SIZE)
}
