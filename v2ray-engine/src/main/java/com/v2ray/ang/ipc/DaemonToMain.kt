package com.v2ray.ang.ipc

sealed interface DaemonToMain {
  data object Running : DaemonToMain

  data object NotRunning : DaemonToMain

  data class StartSuccess(val guid: String? = null) : DaemonToMain

  data class StartFailure(val message: String) : DaemonToMain

  data object StopSuccess : DaemonToMain

  data class DelayMeasured(
    val guid: String?,
    val delayMs: Long?,
  ) : DaemonToMain

  data class ConfigTestResult(
    val guid: String,
    val result: Any?,
  ) : DaemonToMain

  data class ConfigTestProgress(
    val remaining: Int,
    val total: Int,
  ) : DaemonToMain

  data object ConfigTestFinished : DaemonToMain
}
