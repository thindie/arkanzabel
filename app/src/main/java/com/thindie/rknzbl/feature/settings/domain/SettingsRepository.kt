package com.thindie.rknzbl.feature.settings.domain

import com.thindie.rknzbl.uikit.ThemeSwitcher
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Settings feature.
 * Handles theme mode and autosave configuration operations.
 */
interface SettingsRepository {
  /** Get current theme mode: [ThemeSwitcher.Choice.Auto], [ThemeSwitcher.Choice.Light], or [ThemeSwitcher.Choice.Dark] */
  suspend fun getThemeMode(): ThemeSwitcher.Choice?

  /** Set theme mode: [ThemeSwitcher.Choice.Auto], [ThemeSwitcher.Choice.Light], or [ThemeSwitcher.Choice.Dark] */
  fun setThemeMode(mode: ThemeSwitcher.Choice): Boolean

  /** Check if autosave is enabled */
  suspend fun isAutosaveEnabled(): Boolean

  /** Enable or disable autosave */
  suspend fun toggleAutosave(enabled: Boolean): Boolean

  /** Flow of current theme choice, emits whenever theme changes */
  val themeChoice: Flow<ThemeSwitcher.Choice>

  /** Reactive flow for autosave state updates */
  val autosaveEnabled: Flow<Boolean>

  /** Get current MUX enabled status */
  suspend fun isMuxEnabled(): Boolean

  /** Enable or disable MUX */
  suspend fun toggleMux(enabled: Boolean): Boolean

  /** Reactive flow for MUX state updates */
  val muxEnabled: Flow<Boolean>

  /** Check if local storage mode is enabled */
  suspend fun isLocalSaveEnabled(): Boolean

  /** Enable or disable local storage mode */
  suspend fun toggleLocalSave(enabled: Boolean): Boolean

  /** Reactive flow for local save state updates */
  val isLocalSave: Flow<Boolean>

  fun language(): String?

  fun setLanguage(code: String)

  /** Check if start with favorite profiles is enabled */
  fun isStartWithFavoriteProfilesEnabled(): Boolean

  /** Enable or disable start with favorite profiles */
  suspend fun toggleStartWithFavoriteProfiles(enabled: Boolean): Boolean

  /** Reactive flow for start with favorite profiles state updates */
  val startWithFavoriteProfiles: Flow<Boolean>

  // Speed notification
  val speedEnabled: kotlinx.coroutines.flow.Flow<Boolean?>

  fun isSpeedEnabled(): Boolean

  suspend fun toggleSpeed(enabled: Boolean): Boolean

  // Custom source URL
  val customSourceUrl: Flow<String?>

  fun setCustomSourceUrl(url: String)

  val isCustomSourceEnabled: Flow<Boolean>

  fun setCustomSourceEnabled(enabled: Boolean)
}
