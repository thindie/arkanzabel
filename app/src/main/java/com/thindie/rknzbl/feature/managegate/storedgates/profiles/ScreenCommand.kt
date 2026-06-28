package com.thindie.rknzbl.feature.managegate.storedgates.profiles

import com.thindie.rknzbl.engine.Command
import com.v2ray.ang.dto.ConnectionProfile

sealed interface ScreenCommand : Command {
  data object BackRequested : ScreenCommand

  data object Dismissed : ScreenCommand

  data object RequestStoredProfiles : ScreenCommand

  data object StopService : ScreenCommand

  data class EnterMultiDeletionMode(val profile: ConnectionProfile) : ScreenCommand

  data class TogglePendingDelete(val profile: ConnectionProfile) : ScreenCommand

  data object ExitMultiDeletionMode : ScreenCommand

  data class Activate(val profile: ConnectionProfile) : ScreenCommand

  data class Delete(val profile: ConnectionProfile) : ScreenCommand

  data object BatchDelete : ScreenCommand
}
