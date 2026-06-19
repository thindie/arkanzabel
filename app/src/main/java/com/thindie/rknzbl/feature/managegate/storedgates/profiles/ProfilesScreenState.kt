package com.thindie.rknzbl.feature.managegate.storedgates.profiles

import com.thindie.rknzbl.engine.State
import com.v2ray.ang.dto.ConnectionProfile
import com.v2ray.ang.runtime.SpeedtestManager

data class ScreenState(
  val profiles: List<ConnectionProfile> = emptyList(),
  val selected: ConnectionProfile? = null,
  val selectedTestConnectionMessage: SpeedtestManager.SpeedTestResult? = null,
  val selectedProfiles: Set<ConnectionProfile> = emptySet(),
  val selectionMode: Boolean = false,
  val established: Boolean = false,
  val serviceBeingStarted: Boolean? = null,
  val showLoading: Boolean = true,
) : State

sealed interface ScreenCommand : com.thindie.rknzbl.engine.Command {
  data object BackRequested : ScreenCommand

  data object Dismissed : ScreenCommand

  data object RequestStoredProfiles : ScreenCommand

  data object StopService : ScreenCommand

  // Selection mode commands
  data class EnterMultiDeletionMode(val profile: ConnectionProfile) : ScreenCommand

  data class TogglePendingDelete(val profile: ConnectionProfile) : ScreenCommand

  data object ExitMultiDeletionMode : ScreenCommand

  data class Activate(val profile: ConnectionProfile) : ScreenCommand

  data class Delete(val profile: ConnectionProfile) : ScreenCommand

  data object BatchDelete : ScreenCommand
}
