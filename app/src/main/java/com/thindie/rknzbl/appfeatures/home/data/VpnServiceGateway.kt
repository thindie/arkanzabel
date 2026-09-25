package com.thindie.rknzbl.appfeatures.home.data

import com.thindie.engine.core.Log
import com.thindie.engine.core.WorkState
import com.thindie.rknzbl.application.Application
import com.v2ray.ang.ipc.BroadcastersHolder
import com.v2ray.ang.ipc.DaemonToMain
import com.v2ray.ang.ipc.FromMainToDaemon
import com.v2ray.ang.ipc.IpcMainBroadcastReceiver
import com.v2ray.ang.runtime.V2RayServiceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Thin wrapper around [V2RayServiceManager] so [ConnectionProfileRepositoryImpl] can be
 * unit-tested without initializing the native V2Ray core (the manager's static init loads JNI).
 */
interface VpnServiceGateway {
  fun startVService(guid: String)

  fun stopVService()

  fun isRunning(): Boolean

  fun getRunningServerName(): String

  val serviceState: StateFlow<WorkState>
}

internal class V2RayVpnServiceGateway private constructor(
  private val application: Application,
) : VpnServiceGateway {
  private val scope get() = application.applicationScope.coroutineScope

  override val serviceState = MutableStateFlow<WorkState>(if (V2RayServiceManager.isRunning()) WorkState.Running else WorkState.Idle)

  override fun startVService(guid: String) {
    (application as BroadcastersHolder).mainBroadcastReceiver.send(FromMainToDaemon.Start(guid))
  }

  override fun stopVService() {
    (application as BroadcastersHolder).mainBroadcastReceiver.send(FromMainToDaemon.Stop)
  }

  override fun isRunning(): Boolean = serviceState.value is WorkState.Running

  override fun getRunningServerName(): String = V2RayServiceManager.getRunningServerName()

  companion object {
    fun instance(application: Application): VpnServiceGateway {
      val holder = application as BroadcastersHolder
      return V2RayVpnServiceGateway(application).apply {
        holder.mainBroadcastReceiver.startObserving()
        scope.launch {
          IpcMainBroadcastReceiver.events.collect { event ->
            when (event) {
              is DaemonToMain.StartFailure -> {
                Log.w({ "ipc: start == failure" }, "VPN")
                serviceState.value =
                  WorkState.Error(message = event.message)
              }

              is DaemonToMain.Running,
              is DaemonToMain.StartSuccess,
              -> {
                Log.i({ "ipc: running" }, "VPN")
                serviceState.value = WorkState.Running
              }

              is DaemonToMain.NotRunning,
              is DaemonToMain.StopSuccess,
              -> {
                Log.i({ "ipc: stopped" }, "VPN")
                serviceState.value = WorkState.Idle
              }

              is DaemonToMain.DelayMeasured -> {
                Log.i({ "ipc: delay measured ${event.guid}=${event.delayMs}" }, "VPN")
              }

              is DaemonToMain.ConfigTestResult,
              is DaemonToMain.ConfigTestProgress,
              is DaemonToMain.ConfigTestFinished,
              -> {
                Log.i({ "ipc: config test event" }, "VPN")
              }
            }
          }
        }
      }
    }
  }
}
