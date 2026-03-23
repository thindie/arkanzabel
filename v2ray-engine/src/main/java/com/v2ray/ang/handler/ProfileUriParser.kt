package com.v2ray.ang.handler

import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.ProfileItem
import com.v2ray.ang.protocolstringsparsers.Custom
import com.v2ray.ang.protocolstringsparsers.Hysteria2
import com.v2ray.ang.protocolstringsparsers.Shadowsocks
import com.v2ray.ang.protocolstringsparsers.Socks
import com.v2ray.ang.protocolstringsparsers.Trojan
import com.v2ray.ang.protocolstringsparsers.Vless
import com.v2ray.ang.protocolstringsparsers.Vmess
import com.v2ray.ang.protocolstringsparsers.Wireguard
import java.net.URISyntaxException

/** Resolves a single share line (subscription row, clipboard) to [ProfileItem]. */
object ProfileUriParser {

  fun parse(raw: String): ProfileItem? {
    val t = raw.trim()
    if (t.isEmpty() || t.startsWith("#")) return null
    return try {
      when {
        t.startsWith("{") -> Custom.parse(t)
        t.startsWith(AppConfig.VLESS, ignoreCase = true) -> Vless.parse(t)
        t.startsWith(AppConfig.VMESS, ignoreCase = true) ->
          Vmess.parseVmessStd(t) ?: Vmess.parse(t)
        t.startsWith(AppConfig.SHADOWSOCKS, ignoreCase = true) -> Shadowsocks.parse(t)
        t.startsWith(AppConfig.TROJAN, ignoreCase = true) -> Trojan.parse(t)
        t.startsWith(AppConfig.HYSTERIA2, ignoreCase = true) ||
          t.startsWith(AppConfig.HY2, ignoreCase = true) -> Hysteria2.parse(t)
        t.startsWith(AppConfig.SOCKS, ignoreCase = true) -> Socks.parse(t)
        t.startsWith(AppConfig.WIREGUARD, ignoreCase = true) -> Wireguard.parse(t)
        else -> null
      }
    } catch (_: URISyntaxException) {
      null
    }
  }
}
