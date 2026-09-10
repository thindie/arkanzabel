package com.thindie.rknzbl.domain

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

  val speedEnabled: Flow<Boolean?>

  suspend fun toggleSpeed(enabled: Boolean)

  val language: Flow<String?>

  fun setLanguage(code: String)

  // Custom source URL (redesigned design): presence of a non-blank URL means the source is active.

  val customSourceUrl: Flow<String?>

  /** Persists [url]; null/blank clears it and disables the custom source. */
  fun setCustomSourceUrl(url: String?)
}
