package com.thindie.rknzbl.feature.settings.ui

import com.thindie.rknzbl.engine.Command
import com.thindie.rknzbl.uikit.ThemeSwitcher

/**
 * Command sealed interface for the Settings Screen (MVI Pattern).
 */
internal sealed interface SettingsScreenCommand : Command {
  /** Navigate back to previous screen */
  data object Back : SettingsScreenCommand

  /** Set theme mode: [ThemeSwitcher.Choice.Auto], [ThemeSwitcher.Choice.Light], or [ThemeSwitcher.Choice.Dark] */
  data class SetThemeMode(val choice: ThemeSwitcher.Choice) : SettingsScreenCommand

  /** Toggle autosave on/off */
  data object ToggleAutosave : SettingsScreenCommand
}
