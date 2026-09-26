package com.thindie.rknzbl.appfeatures.home.data

import com.thindie.engine.core.WorkState
import com.thindie.rknzbl.application.Application
import com.v2ray.ang.ipc.BroadcastersHolder
import com.v2ray.ang.ipc.FromMainToDaemon
import com.v2ray.ang.runtime.V2RayServiceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

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
      return V2RayVpnServiceGateway(application)
    }
  }
}
