package com.v2ray.ang.contracts

/** v2rayNG parity: userspace TUN → SOCKS (e.g. hev-socks5-tunnel). */
interface Tun2SocksControl {
  fun startTun2Socks()

  fun stopTun2Socks()
}
