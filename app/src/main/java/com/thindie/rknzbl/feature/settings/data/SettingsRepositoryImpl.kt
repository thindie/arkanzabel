package com.thindie.rknzbl.feature.settings.data

import com.thindie.engine.uikit.ThemeSwitcher
import com.thindie.rknzbl.feature.settings.data.theme.toChoice
import com.thindie.rknzbl.feature.settings.data.theme.toStorageString
import com.thindie.rknzbl.feature.settings.domain.SettingsRepository
import com.v2ray.ang.AppConfig
import com.v2ray.ang.runtime.KeyValueStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach

class SettingsRepositoryImpl(
  private val storage: KeyValueStorage,
) : SettingsRepository {
  override suspend fun getThemeMode(): ThemeSwitcher.Choice? = storage.getThemeMode()?.toChoice()

  private val _themeChoice = MutableStateFlow(storage.getThemeMode()?.toChoice())

  override val themeChoice =
    _themeChoice
      .filterNotNull()
      .onEach { storage.setThemeMode(it.toStorageString()) }

  override fun setThemeMode(mode: ThemeSwitcher.Choice): Boolean {
    _themeChoice.value = mode
    return true
  }

  private val _autosaveEnabled = MutableStateFlow(storage.isAutosaveEnabled())

  override val autosaveEnabled =
    _autosaveEnabled
      .onEach { storage.setAutosaveMode(it) }

  override suspend fun isAutosaveEnabled(): Boolean = storage.isAutosaveEnabled()

  override suspend fun toggleAutosave(enabled: Boolean): Boolean {
    _autosaveEnabled.value = enabled
    return true
  }

  // MUX support
  private val _muxEnabled = MutableStateFlow(storage.decodeSettingsBool(AppConfig.PREF_MUX_ENABLED, false))

  override val muxEnabled =
    _muxEnabled
      .onEach { storage.encodeSettings(AppConfig.PREF_MUX_ENABLED, it) }

  override fun language(): String? {
    return storage.decodeSettingsString(AppConfig.PREF_LANGUAGE)
  }

  override fun setLanguage(code: String) {
    storage.encodeSettings(AppConfig.PREF_LANGUAGE, code)
  }

  override suspend fun isMuxEnabled(): Boolean = storage.decodeSettingsBool(AppConfig.PREF_MUX_ENABLED, false)

  override suspend fun toggleMux(enabled: Boolean): Boolean {
    _muxEnabled.value = enabled
    return true
  }

  // Fragment support (Recommendation #5 — global packet fragmentation, applied by OutboundConfigStep on TLS/REALITY outbounds)
  private val _fragmentEnabled = MutableStateFlow(storage.decodeSettingsBool(AppConfig.PREF_FRAGMENT_ENABLED, false))

  override val fragmentEnabled =
    _fragmentEnabled
      .onEach { storage.encodeSettings(AppConfig.PREF_FRAGMENT_ENABLED, it) }

  override suspend fun isFragmentEnabled(): Boolean = storage.decodeSettingsBool(AppConfig.PREF_FRAGMENT_ENABLED, false)

  override suspend fun toggleFragment(enabled: Boolean): Boolean {
    _fragmentEnabled.value = enabled
    return true
  }

  private val _fragmentLength =
    MutableStateFlow(
      storage.decodeSettingsString(AppConfig.PREF_FRAGMENT_LENGTH),
    )

  override fun setFragmentLength(length: String) {
    _fragmentLength.value = length
    storage.encodeSettings(AppConfig.PREF_FRAGMENT_LENGTH, length)
  }

  private val _fragmentInterval =
    MutableStateFlow(
      storage.decodeSettingsString(AppConfig.PREF_FRAGMENT_INTERVAL),
    )

  override val fragmentInterval =
    _fragmentInterval.mapNotNull { it?.ifBlank { null } }
      .onEach { storage.encodeSettings(AppConfig.PREF_FRAGMENT_INTERVAL, it) }

  override fun setFragmentInterval(interval: String) {
    _fragmentInterval.value = interval
    storage.encodeSettings(AppConfig.PREF_FRAGMENT_INTERVAL, interval)
  }

  // Local storage mode support
  private val _isLocalSave = MutableStateFlow(storage.isLocalSaveEnabled())

  override val isLocalSave =
    _isLocalSave
      .onEach { storage.setLocalSaveMode(it) }

  override suspend fun isLocalSaveEnabled(): Boolean = storage.isLocalSaveEnabled()

  override suspend fun toggleLocalSave(enabled: Boolean): Boolean {
    _isLocalSave.value = enabled
    return true
  }

  // Start with favorite profiles support
  private val _startWithFavoriteProfiles = MutableStateFlow(storage.isLocalSaveEnabled())

  override val startWithFavoriteProfiles =
    _startWithFavoriteProfiles
      .onEach { storage.setLocalSaveMode(it) }

  override fun isStartWithFavoriteProfilesEnabled(): Boolean = storage.isLocalSaveEnabled()

  override suspend fun toggleStartWithFavoriteProfiles(enabled: Boolean): Boolean {
    _startWithFavoriteProfiles.value = enabled
    return true
  }

  // Speed notification support
  private val _speedEnabled = MutableStateFlow(storage.decodeSettingsBool(AppConfig.PREF_SPEED_ENABLED, false))

  override val speedEnabled =
    _speedEnabled
      .onEach { storage.encodeSettings(AppConfig.PREF_SPEED_ENABLED, it) }

  override fun isSpeedEnabled(): Boolean = storage.decodeSettingsBool(AppConfig.PREF_SPEED_ENABLED, false)

  override suspend fun toggleSpeed(enabled: Boolean): Boolean {
    _speedEnabled.value = enabled
    return true
  }

  // Custom source URL support
  private val _customSourceUrl = MutableStateFlow(storage.getCustomSourceUrl())

  override val customSourceUrl =
    _customSourceUrl
      .filterNotNull()
      .onEach(storage::setCustomSourceUrl)

  private val customSourceEnabledInternal = MutableStateFlow(storage.isCustomSourceEnabled())

  override val isCustomSourceEnabled =
    customSourceEnabledInternal
      .onEach(storage::setCustomSourceEnabled)

  override fun setCustomSourceUrl(url: String) {
    _customSourceUrl.value = url
  }

  override fun setCustomSourceEnabled(enabled: Boolean) {
    customSourceEnabledInternal.value = enabled
  }

  // Reality masquerade (show) support — global toggle for TLS/REALITY outbounds
  private val _realityShowEnabled = MutableStateFlow(storage.decodeSettingsBool(AppConfig.PREF_REALITY_SHOW_ENABLED, AppConfig.REALITY_SHOW_ENABLED))

  override fun isRealityShowEnabled(): Boolean = storage.decodeSettingsBool(AppConfig.PREF_REALITY_SHOW_ENABLED, AppConfig.REALITY_SHOW_ENABLED)

  override suspend fun toggleRealityShow(enabled: Boolean): Boolean {
    _realityShowEnabled.value = enabled
    storage.encodeSettings(AppConfig.PREF_REALITY_SHOW_ENABLED, enabled)
    return true
  }
}
