package com.thindie.rknzbl.feature.settings.domain

import com.thindie.rknzbl.uikit.ThemeSwitcher

/**
 * Repository interface for Settings feature.
 * Handles theme mode and autosave configuration operations.
 */
interface SettingsRepository {
  /** Get current theme mode: [ThemeSwitcher.Choice.Auto], [ThemeSwitcher.Choice.Light], or [ThemeSwitcher.Choice.Dark] */
  suspend fun getThemeMode(): ThemeSwitcher.Choice?

  /** Set theme mode: [ThemeSwitcher.Choice.Auto], [ThemeSwitcher.Choice.Light], or [ThemeSwitcher.Choice.Dark] */
  suspend fun setThemeMode(mode: ThemeSwitcher.Choice): Boolean

  /** Check if autosave is enabled */
  suspend fun isAutosaveEnabled(): Boolean

  /** Enable or disable autosave */
  suspend fun toggleAutosave(enabled: Boolean): Boolean
}
