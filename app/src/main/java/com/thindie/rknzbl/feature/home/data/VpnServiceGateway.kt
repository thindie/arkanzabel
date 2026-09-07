package com.thindie.rknzbl.feature.home.data

import android.content.Context
import com.v2ray.ang.runtime.V2RayServiceManager

/**
 * Thin wrapper around [V2RayServiceManager] so [ConnectionProfileRepositoryImpl] can be
 * unit-tested without initializing the native V2Ray core (the manager's static init loads JNI).
 */
interface VpnServiceGateway {
  fun startVService(
    context: Context,
    guid: String,
  )

  fun stopVService(context: Context)

  fun isRunning(): Boolean

  fun getRunningServerName(): String
}

class V2RayVpnServiceGateway : VpnServiceGateway {
  override fun startVService(
    context: Context,
    guid: String,
  ) = V2RayServiceManager.startVService(context, guid)

  override fun stopVService(context: Context) = V2RayServiceManager.stopVService(context)

  override fun isRunning(): Boolean = V2RayServiceManager.isRunning()

  override fun getRunningServerName(): String = V2RayServiceManager.getRunningServerName()
}
