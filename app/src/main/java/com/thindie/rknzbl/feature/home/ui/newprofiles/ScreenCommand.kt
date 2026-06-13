package com.thindie.rknzbl.feature.home.ui.newprofiles

import com.thindie.rknzbl.engine.Command
import com.v2ray.ang.dto.ConnectionProfile

sealed interface ScreenCommand : Command {
  data object Back : ScreenCommand

  data object Start : ScreenCommand

  data object Refresh : ScreenCommand

  data object Stop : ScreenCommand

  data object Choose : ScreenCommand

  data class Select(val profile: ConnectionProfile) : ScreenCommand

  data class Save(val profile: ConnectionProfile) : ScreenCommand

  data object Dismissed : ScreenCommand

  data object OpenPerAppProxy : ScreenCommand
}
