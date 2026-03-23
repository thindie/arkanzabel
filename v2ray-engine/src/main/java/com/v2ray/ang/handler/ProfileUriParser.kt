package com.v2ray.ang.handler

import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.ProfileItem
import com.v2ray.ang.fmt.CustomFmt
import com.v2ray.ang.fmt.Hysteria2Fmt
import com.v2ray.ang.fmt.ShadowsocksFmt
import com.v2ray.ang.fmt.SocksFmt
import com.v2ray.ang.fmt.TrojanFmt
import com.v2ray.ang.fmt.VlessFmt
import com.v2ray.ang.fmt.VmessFmt
import com.v2ray.ang.fmt.WireguardFmt
import java.net.URISyntaxException

/** Resolves a single share line (subscription row, clipboard) to [ProfileItem]. */
object ProfileUriParser {

  fun parse(raw: String): ProfileItem? {
    val t = raw.trim()
    if (t.isEmpty() || t.startsWith("#")) return null
    return try {
      when {
        t.startsWith("{") -> CustomFmt.parse(t)
        t.startsWith(AppConfig.VLESS, ignoreCase = true) -> VlessFmt.parse(t)
        t.startsWith(AppConfig.VMESS, ignoreCase = true) ->
          VmessFmt.parseVmessStd(t) ?: VmessFmt.parse(t)
        t.startsWith(AppConfig.SHADOWSOCKS, ignoreCase = true) -> ShadowsocksFmt.parse(t)
        t.startsWith(AppConfig.TROJAN, ignoreCase = true) -> TrojanFmt.parse(t)
        t.startsWith(AppConfig.HYSTERIA2, ignoreCase = true) ||
          t.startsWith(AppConfig.HY2, ignoreCase = true) -> Hysteria2Fmt.parse(t)
        t.startsWith(AppConfig.SOCKS, ignoreCase = true) -> SocksFmt.parse(t)
        t.startsWith(AppConfig.WIREGUARD, ignoreCase = true) -> WireguardFmt.parse(t)
        else -> null
      }
    } catch (_: URISyntaxException) {
      null
    }
  }
}
