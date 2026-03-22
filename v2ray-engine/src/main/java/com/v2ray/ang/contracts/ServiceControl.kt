package com.v2ray.ang.contracts

import android.app.Service

/**
 * Narrow seam for the running VPN/proxy [Service]
 */
interface ServiceControl {
    val service: Service
    fun start()
    fun stop()

    /**
     * Platform [android.net.VpnService.protect]: keep traffic on this socket fd off the VPN tunnel.
     * @return whether the call succeeded on this API level.
     */
    fun protectSocket(fd: Int): Boolean
}
