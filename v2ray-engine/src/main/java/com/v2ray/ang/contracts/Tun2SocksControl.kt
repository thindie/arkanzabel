package com.v2ray.ang.contracts

/** v2rayNG parity: userspace TUN → SOCKS path (e.g. hev-socks5-tunnel). */
interface Tun2SocksControl {
    fun start()
    fun stop()
}
