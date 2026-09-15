package com.thindie.rknzbl.appfeatures.profiles.ui

import com.thindie.engine.core.Command
import com.v2ray.ang.dto.ConnectionProfile

sealed interface ScreenCommand : Command {
  data object LoadProfiles : ScreenCommand

  data object RefreshProfiles : ScreenCommand

  data class SelectTab(val index: Int) : ScreenCommand

  data class ConnectProfile(val profile: ConnectionProfile) : ScreenCommand

  data object OpenDeleteSavedProfiles : ScreenCommand
}
