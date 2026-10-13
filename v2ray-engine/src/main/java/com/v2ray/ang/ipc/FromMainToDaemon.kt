package com.v2ray.ang.ipc

import com.v2ray.ang.dto.ConnectionProfile

sealed interface FromMainToDaemon {
  data object RegisterClient : FromMainToDaemon

  data object UnregisterClient : FromMainToDaemon

  data class Start(val profile: ConnectionProfile) : FromMainToDaemon

  data object Stop : FromMainToDaemon

  data object Restart : FromMainToDaemon

  data class MeasureProfile(
    val profile: ConnectionProfile,
    val requestId: String,
  ) : FromMainToDaemon

  data object SaveProfile : FromMainToDaemon
}
