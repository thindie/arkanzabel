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

  fun language(): String?

  fun setLanguage(code: String)
}
