package com.thindie.rknzbl.appfeatures.settings.domain

import com.thindie.engine.uikit.ThemeSwitcher
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
  val autosaveEnabled: Flow<Boolean>

  suspend fun toggleAutosave(enabled: Boolean)

  val themeChoice: Flow<ThemeSwitcher.Choice>

  val muxEnabled: Flow<Boolean>

  suspend fun toggleMux(enabled: Boolean)

  val isLocalSave: Flow<Boolean>

  suspend fun toggleLocalSave(enabled: Boolean)

  val useNewDesign: Flow<Boolean>

  suspend fun toggleUseNewDesign(enabled: Boolean)

  fun getUseNewDesignSync(): Boolean

  val speedEnabled: Flow<Boolean?>

  suspend fun toggleSpeed(enabled: Boolean)

  // Language - read from storage on subscription start
  fun getLanguageSync(): String?

  val language: Flow<String?>

  fun setLanguage(code: String)

  // Custom source URL

  val customSourceUrl: Flow<String?>

  fun setCustomSourceUrl(url: String)

  val isCustomSourceEnabled: Flow<Boolean>

  fun setCustomSourceEnabled(enabled: Boolean)
}
