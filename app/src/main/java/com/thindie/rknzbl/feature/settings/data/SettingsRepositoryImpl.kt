package com.thindie.rknzbl.feature.settings.data

import com.thindie.rknzbl.feature.settings.data.theme.toChoice
import com.thindie.rknzbl.feature.settings.data.theme.toStorageString
import com.thindie.rknzbl.feature.settings.domain.SettingsRepository
import com.thindie.rknzbl.uikit.ThemeSwitcher
import com.v2ray.ang.AppConfig
import com.v2ray.ang.runtime.KeyValueStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart

class SettingsRepositoryImpl(
  private val storage: KeyValueStorage,
) : SettingsRepository {
  override suspend fun getThemeMode(): ThemeSwitcher.Choice? = storage.getThemeMode()?.toChoice()

  private val _themeChoice = MutableStateFlow<ThemeSwitcher.Choice?>(null)

  override val themeChoice =
    _themeChoice
      .filterNotNull()
      .onStart { storage.getThemeMode()?.toChoice()?.let { emit(it) } }
      .onEach {
        storage.setThemeMode(it.toStorageString())
      }

  override fun setThemeMode(mode: ThemeSwitcher.Choice): Boolean {
    _themeChoice.value = mode
    return true
  }

  private val _autosaveEnabled = MutableStateFlow<Boolean?>(null)

  override val autosaveEnabled =
    _autosaveEnabled
      .filterNotNull()
      .onStart { emit(storage.isAutosaveEnabled()) }
      .onEach { storage.setAutosaveMode(it) }

  override suspend fun isAutosaveEnabled(): Boolean = storage.isAutosaveEnabled()

  override suspend fun toggleAutosave(enabled: Boolean): Boolean {
    _autosaveEnabled.value = enabled
    return true
  }

  // MUX support
  private val _muxEnabled = MutableStateFlow<Boolean?>(null)

  override val muxEnabled =
    _muxEnabled
      .filterNotNull()
      .onStart { storage.decodeSettingsBool(AppConfig.PREF_MUX_ENABLED)?.let { emit(it) } }
      .onEach {
        storage.encodeSettings(AppConfig.PREF_MUX_ENABLED, it)
      }

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

  // Local storage mode support
  private val _isLocalSave = MutableStateFlow<Boolean?>(null)

  override val isLocalSave =
    _isLocalSave
      .filterNotNull()
      .onStart { emit(storage.isLocalSaveEnabled()) }
      .onEach { storage.setLocalSaveMode(it) }

  override suspend fun isLocalSaveEnabled(): Boolean = storage.isLocalSaveEnabled()

  override suspend fun toggleLocalSave(enabled: Boolean): Boolean {
    _isLocalSave.value = enabled
    return true
  }

  // Start with favorite profiles support
  private val _startWithFavoriteProfiles = MutableStateFlow<Boolean?>(null)

  override val startWithFavoriteProfiles =
    _startWithFavoriteProfiles
      .filterNotNull()
      .onStart { emit(storage.isLocalSaveEnabled()) }
      .onEach { storage.setLocalSaveMode(it) }

  override fun isStartWithFavoriteProfilesEnabled(): Boolean = storage.isLocalSaveEnabled()

  override suspend fun toggleStartWithFavoriteProfiles(enabled: Boolean): Boolean {
    _startWithFavoriteProfiles.value = enabled
    return true
  }

  // Speed notification support
  private val _speedEnabled = MutableStateFlow<Boolean?>(null)

  override val speedEnabled =
    _speedEnabled
      .filterNotNull()
      .onStart { storage.decodeSettingsBool(AppConfig.PREF_SPEED_ENABLED)?.let { emit(it) } }
      .onEach {
        storage.encodeSettings(AppConfig.PREF_SPEED_ENABLED, it)
      }

  override fun isSpeedEnabled(): Boolean = storage.decodeSettingsBool(AppConfig.PREF_SPEED_ENABLED, false)

  override suspend fun toggleSpeed(enabled: Boolean): Boolean {
    _speedEnabled.value = enabled
    return true
  }
}
