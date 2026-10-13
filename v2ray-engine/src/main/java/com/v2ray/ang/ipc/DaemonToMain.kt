package com.v2ray.ang.ipc

import com.v2ray.ang.dto.ConnectionProfile

sealed interface DaemonToMain {
  data object Running : DaemonToMain

  data object NotRunning : DaemonToMain

  data class StartSuccess(val profile: ConnectionProfile) : DaemonToMain

  data class StartFailure(val message: String) : DaemonToMain

  data object StopSuccess : DaemonToMain

  data class ProfileMeasured(
    val profile: ConnectionProfile,
    val requestId: String,
  ) : DaemonToMain

  data object SaveProfile : DaemonToMain
}
