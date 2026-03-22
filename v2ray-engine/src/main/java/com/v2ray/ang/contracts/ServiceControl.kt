package com.v2ray.ang.contracts

import android.app.Service

/** v2rayNG parity: control surface used by [com.v2ray.ang.handler.V2RayServiceManager] and VPN/proxy services. */
interface ServiceControl {
  fun getService(): Service

  fun startService()

  fun stopService()

  /** Maps to [android.net.VpnService.protect]. */
  fun vpnProtect(socket: Int): Boolean
}
