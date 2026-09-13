package com.thindie.rknzbl.domain

import com.thindie.engine.uikit.ThemeSwitcher
import com.v2ray.ang.dto.WebDavConfig
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
  val autosaveEnabled: Flow<Boolean>

  suspend fun toggleAutosave(enabled: Boolean)

  val themeChoice: Flow<ThemeSwitcher.Choice>

  val muxEnabled: Flow<Boolean>

  suspend fun toggleMux(enabled: Boolean)

  val isLocalSave: Flow<Boolean>

  suspend fun toggleLocalSave(enabled: Boolean)

  val speedEnabled: Flow<Boolean?>

  suspend fun toggleSpeed(enabled: Boolean)

  val language: Flow<String?>

  fun setLanguage(code: String)

  // Custom source URL (redesigned design): presence of a non-blank URL means the source is active.

  val customSourceUrl: Flow<String?>

  /** Persists [url]; null/blank clears it and disables the custom source. */
  fun setCustomSourceUrl(url: String?)

  // --- WebDAV storage config (URL / login / password); null means not configured ---
  val webDavConfig: Flow<WebDavConfig?>

  /** Persists [config]; null clears the stored key and disables remote storage. */
  fun setWebDavConfig(config: WebDavConfig?)

  // --- WebDAV: use built-in default config instead of manual entry ---
  val webDavUseDefaults: Flow<Boolean>

  suspend fun toggleWebDavUseDefaults(enabled: Boolean)
}
