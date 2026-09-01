package com.thindie.rknzbl.feature.settings.data

import com.thindie.engine.uikit.ThemeSwitcher
import com.thindie.rknzbl.feature.settings.data.theme.toChoice
import com.thindie.rknzbl.feature.settings.data.theme.toStorageString
import com.thindie.rknzbl.feature.settings.domain.SettingsRepository
import com.thindie.rknzbl.feature.settings.ui.SniffingPortRange
import com.thindie.rknzbl.feature.settings.ui.SniffingTarget
import com.thindie.rknzbl.feature.settings.ui.toSniffingPortRange
import com.thindie.rknzbl.feature.settings.ui.toSniffingTarget
import com.thindie.rknzbl.feature.settings.ui.toStorageString
import com.v2ray.ang.AppConfig
import com.v2ray.ang.runtime.KeyValueStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.mapNotNull

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

  // --- Fragment support (Recommendation #5 — global packet fragmentation on TLS/REALITY outbounds) ---
  private val fragmentEnabledSetting =
    Setting<Boolean>(
      read = { storage.decodeSettingsBool(AppConfig.PREF_FRAGMENT_ENABLED, false) },
      write = { storage.encodeSettings(AppConfig.PREF_FRAGMENT_ENABLED, it) },
    )
  override val fragmentEnabled: Flow<Boolean> get() = fragmentEnabledSetting.flow

  override suspend fun toggleFragment(enabled: Boolean) {
    fragmentEnabledSetting.set(enabled)
  }

  private val fragmentIntervalSetting =
    Setting<String>(
      read = { storage.decodeSettingsString(AppConfig.PREF_FRAGMENT_INTERVAL).orEmpty() },
      write = { storage.encodeSettings(AppConfig.PREF_FRAGMENT_INTERVAL, it) },
    )
  override val fragmentInterval: Flow<String?> get() =
    fragmentIntervalSetting.flow.mapNotNull { it.ifBlank { null } }

  override fun setFragmentInterval(interval: String) {
    fragmentIntervalSetting.set(interval)
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

  // --- Start with favourite profiles support (own storage key, independent of local-save) ---
  private val startWithFavouritesSetting =
    Setting<Boolean>(
      read = { storage.decodeSettingsBool(AppConfig.PREF_START_WITH_FAVOURITES) },
      write = { storage.encodeSettings(AppConfig.PREF_START_WITH_FAVOURITES, it) },
    )
  override val startWithFavoriteProfiles: Flow<Boolean> get() = startWithFavouritesSetting.flow

  override suspend fun toggleStartWithFavoriteProfiles(enabled: Boolean) {
    startWithFavouritesSetting.set(enabled)
  }

  override fun getStartWithFavoriteProfilesSync(): Boolean = startWithFavouritesSetting.getSync()

  // --- Feature toggle: bottom-navigation home design (default off) ---
  private val useNewDesignSetting =
    Setting<Boolean>(
      read = { storage.decodeSettingsBool(AppConfig.PREF_USE_NEW_DESIGN, false) },
      write = { storage.encodeSettings(AppConfig.PREF_USE_NEW_DESIGN, it) },
    )
  override val useNewDesign: Flow<Boolean> get() = useNewDesignSetting.flow

  override suspend fun toggleUseNewDesign(enabled: Boolean) {
    useNewDesignSetting.set(enabled)
  }

  override fun getUseNewDesignSync(): Boolean = useNewDesignSetting.getSync()

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

  override fun getLanguageSync(): String = languageSetting.getSync()

  // --- Custom source URL support ---
  private val customSourceUrlSetting =
    Setting<String>(
      read = { storage.getCustomSourceUrl().orEmpty() },
      write = { storage.setCustomSourceUrl(it) },
    )
  override val customSourceUrl: Flow<String?> get() = customSourceUrlSetting.flow

  override fun setCustomSourceUrl(url: String) {
    customSourceUrlSetting.set(url)
  }

  private val customSourceEnabledSetting =
    Setting<Boolean>(
      read = { storage.isCustomSourceEnabled() },
      write = { storage.setCustomSourceEnabled(it) },
    )
  override val isCustomSourceEnabled: Flow<Boolean> get() = customSourceEnabledSetting.flow

  override fun setCustomSourceEnabled(enabled: Boolean) {
    customSourceEnabledSetting.set(enabled)
  }

  private val forceProfileMeasureSetting =
    Setting<Boolean>(
      read = { storage.decodeSettingsBool(AppConfig.PREF_FORCE_PROFILE_MEASUREMENT) },
      write = { storage.encodeSettings(AppConfig.PREF_FORCE_PROFILE_MEASUREMENT, it) },
    )

  override val forceProfileMeasure: Flow<Boolean> = forceProfileMeasureSetting.flow

  override fun setForceProfileMeasure(enabled: Boolean) {
    forceProfileMeasureSetting.set(enabled)
  }

  // --- Reality masquerade (show) support — global toggle for TLS/REALITY outbounds ---
  private val realityShowSetting =
    Setting<Boolean>(
      read = {
        storage.decodeSettingsBool(
          AppConfig.PREF_REALITY_SHOW_ENABLED,
          AppConfig.REALITY_SHOW_ENABLED,
        )
      },
      write = { storage.encodeSettings(AppConfig.PREF_REALITY_SHOW_ENABLED, it) },
    )
  override val realityShowEnabled: Flow<Boolean> get() = realityShowSetting.flow

  override suspend fun toggleRealityShow(enabled: Boolean) {
    realityShowSetting.set(enabled)
  }

  // --- Sniffing target protocol support ---
  private val sniffingTargetSetting =
    Setting<SniffingTarget?>(
      read = {
        storage
          .decodeSettingsString(AppConfig.PREF_SNIFFING_TARGET)
          ?.let(::toSniffingTarget)
      },
      write = { storage.encodeSettings(AppConfig.PREF_SNIFFING_TARGET, it?.toStorageString()) },
    )

  override fun sniffingTarget(): Flow<SniffingTarget?> = sniffingTargetSetting.flow

  override fun setSniffingTarget(target: SniffingTarget) {
    sniffingTargetSetting.set(target)
  }

  // --- Sniffing port-range support ---
  private val sniffingPortRangeSetting =
    Setting<SniffingPortRange?>(
      read = {
        storage
          .decodeSettingsString(AppConfig.PREF_SNIFFING_PORT_RANGE)
          ?.let(::toSniffingPortRange)
      },
      write = { storage.encodeSettings(AppConfig.PREF_SNIFFING_PORT_RANGE, it?.toStorageString()) },
    )

  override fun sniffingPortRange(): Flow<SniffingPortRange?> = sniffingPortRangeSetting.flow

  override fun setSniffingPortRange(range: SniffingPortRange) {
    sniffingPortRangeSetting.set(range)
  }
}
