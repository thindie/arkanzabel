package com.thindie.rknzbl.feature.home.ui.select

import com.thindie.rknzbl.engine.Command

internal sealed interface ScreenCommand : Command {
  data object Home : ScreenCommand

  data object New : ScreenCommand

  data object Settings : ScreenCommand

  data object PerAppProxy : ScreenCommand

  data object Back : ScreenCommand

  data object FetchAutoSaved : ScreenCommand

  data object DismissAutoSaved : ScreenCommand
}
