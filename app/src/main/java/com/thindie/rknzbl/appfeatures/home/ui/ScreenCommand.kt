package com.thindie.rknzbl.appfeatures.home.ui

import com.thindie.engine.core.Command

internal sealed interface ScreenCommand : Command {
  data object ToggleConnect : ScreenCommand

  data object RefreshProfiles : ScreenCommand
}
