package com.v2ray.ang.runtimebuilder

import com.v2ray.ang.dto.ConnectionProfile
import com.v2ray.ang.dto.V2rayConfig.Outbound
import com.v2ray.ang.enums.Protocol
import com.v2ray.ang.protocolstringsparsers.Http
import com.v2ray.ang.protocolstringsparsers.Hysteria2
import com.v2ray.ang.protocolstringsparsers.Shadowsocks
import com.v2ray.ang.protocolstringsparsers.Socks
import com.v2ray.ang.protocolstringsparsers.Trojan
import com.v2ray.ang.protocolstringsparsers.Vless
import com.v2ray.ang.protocolstringsparsers.Vmess
import com.v2ray.ang.protocolstringsparsers.Wireguard

/**
 * Maps a stored [ConnectionProfile] to Xray [Outbound] via protocol-specific parsers.
 * Keeps [com.v2ray.ang.runtime.V2rayConfigManager] free of direct parser dependencies for this path.
 */
object ConnectionProfileToOutboundMapper {

  fun map(connectionProfile: ConnectionProfile): Outbound? {
    return when (connectionProfile.protocol) {
      Protocol.Vmess -> Vmess.toOutbound(connectionProfile)
      Protocol.Custom -> null
      Protocol.ShadowSocks -> Shadowsocks.toOutbound(connectionProfile)
      Protocol.Socks -> Socks.toOutbound(connectionProfile)
      Protocol.Vless -> Vless.toOutbound(connectionProfile)
      Protocol.Trojan -> Trojan.toOutbound(connectionProfile)
      Protocol.WireGuard -> Wireguard.toOutbound(connectionProfile)
      Protocol.Hysteria2 -> Hysteria2.toOutbound(connectionProfile)
      Protocol.Http -> Http.toOutbound(connectionProfile)
      Protocol.PolicyGroup -> null
      else -> null
    }
  }
}
