package com.thindie.rknzbl.feature.settings.ui

import com.thindie.engine.core.Command

internal sealed interface ScreenCommand : Command {
  data object Back : ScreenCommand

  data object ToggleAutosave : ScreenCommand

  // Language selection commands
  data class SelectLanguage(val languageCode: String) : ScreenCommand

  // MUX toggle command
  data object ToggleMux : ScreenCommand

  /** Global fragment toggle (Recommendation #5) */
  data object ToggleFragment : ScreenCommand

  /** Set global fragment interval string */
  data class SetFragmentInterval(val interval: String) : ScreenCommand

  // Local storage mode toggle command
  data object ToggleStorageMode : ScreenCommand

  // Start with favorite profiles toggle
  data object StartWithFavoriteProfiles : ScreenCommand

  // Speed notification toggle
  data object ToggleSpeed : ScreenCommand

  // Custom source URL toggle
  data object ToggleCustomSource : ScreenCommand

  // Set custom source URL
  data class SetCustomSourceUrl(val url: String) : ScreenCommand
}
