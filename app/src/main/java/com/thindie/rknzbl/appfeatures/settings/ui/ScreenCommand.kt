package com.thindie.rknzbl.appfeatures.settings.ui

import com.thindie.engine.core.Command

internal sealed interface ScreenCommand : Command {
  data object Back : ScreenCommand

  data object ToggleAutosave : ScreenCommand

  data class SelectLanguage(val languageCode: String) : ScreenCommand

  data object ToggleMux : ScreenCommand

  data object ToggleStorageMode : ScreenCommand

  data object ToggleSpeed : ScreenCommand

  data object ToggleNewDesign : ScreenCommand

  data object ToggleCustomSource : ScreenCommand

  data object OpenWebdav : ScreenCommand

  data object OpenPerAppProxy : ScreenCommand

  data object OpenHelp : ScreenCommand
}
