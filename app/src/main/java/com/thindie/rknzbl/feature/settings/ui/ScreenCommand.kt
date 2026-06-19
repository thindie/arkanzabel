package com.thindie.rknzbl.feature.settings.ui

import com.thindie.rknzbl.engine.Command

internal sealed interface ScreenCommand : Command {
  data object Back : ScreenCommand

  data object ToggleAutosave : ScreenCommand

  // Language selection commands
  data class SelectLanguage(val languageCode: String) : ScreenCommand

  // MUX toggle command
  data object ToggleMux : ScreenCommand
}
