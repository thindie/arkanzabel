package com.v2ray.ang.ipc

sealed interface FromMainToDaemon {
  data object RegisterClient : FromMainToDaemon

  data object UnregisterClient : FromMainToDaemon

  data class Start(val guid: String? = null) : FromMainToDaemon

  data object Stop : FromMainToDaemon

  data object Restart : FromMainToDaemon

  data class MeasureDelay(val guid: String? = null) : FromMainToDaemon

  data object SaveProfile : FromMainToDaemon
}
