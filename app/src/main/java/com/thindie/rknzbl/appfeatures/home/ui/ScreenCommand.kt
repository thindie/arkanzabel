package com.thindie.rknzbl.appfeatures.home.ui

import com.thindie.engine.core.Command

internal sealed interface ScreenCommand : Command {
  /** Loads profiles and measures the best one on screen start. */
  data object LoadProfiles : ScreenCommand

  data object Home : ScreenCommand

  data object New : ScreenCommand

  data object PerAppProxy : ScreenCommand

  data object ToggleConnect : ScreenCommand
}
