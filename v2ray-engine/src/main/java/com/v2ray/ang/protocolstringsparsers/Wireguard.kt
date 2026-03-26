package com.v2ray.ang.protocolstringsparsers

import com.v2ray.ang.AppConfig
import com.v2ray.ang.AppConfig.WIREGUARD_LOCAL_ADDRESS_V4
import com.v2ray.ang.dto.ConnectionProfile
import com.v2ray.ang.dto.V2rayConfig.Outbound
import com.v2ray.ang.enums.Protocol
import com.v2ray.ang.extension.idnHost
import com.v2ray.ang.extension.nullIfBlank
import com.v2ray.ang.extension.removeWhiteSpace
import com.v2ray.ang.runtime.V2rayConfigManager
import com.v2ray.ang.util.Utils
import java.net.URI

object Wireguard : ProtocolParser() {
  fun parse(str: String): ConnectionProfile? {
    val uri = URI(Utils.fixIllegalUrl(str))
    if (uri.rawQuery.isNullOrEmpty()) return null
    val queryParam = getQueryParam(uri)

    return ConnectionProfile(
      protocol = Protocol.WireGuard,
      remarks = Utils.decodeURIComponent(uri.fragment.orEmpty()).let { it.ifEmpty { "none" } },
      server = uri.idnHost,
      serverPort = uri.port.toString(),
      secretKey = uri.userInfo.orEmpty(),
      localAddress = queryParam["address"] ?: WIREGUARD_LOCAL_ADDRESS_V4,
      publicKey = queryParam["publickey"].orEmpty(),
      preSharedKey = queryParam["presharedkey"]?.nullIfBlank(),
      mtu = Utils.parseInt(queryParam["mtu"] ?: AppConfig.WIREGUARD_LOCAL_MTU),
      reserved = queryParam["reserved"] ?: "0,0,0",
    )
  }

  fun parseWireguardConfFile(str: String): ConnectionProfile? {
    val interfaceParams: MutableMap<String, String> = mutableMapOf()
    val peerParams: MutableMap<String, String> = mutableMapOf()

    var currentSection: String? = null

    str.lines().forEach { line ->
      val trimmedLine = line.trim()

      if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) {
        return@forEach
      }

      when {
        trimmedLine.startsWith("[Interface]", ignoreCase = true) -> currentSection = "Interface"
        trimmedLine.startsWith("[Peer]", ignoreCase = true) -> currentSection = "Peer"
        else -> {
          if (currentSection != null) {
            val parts = trimmedLine.split("=", limit = 2).map { it.trim() }
            if (parts.size == 2) {
              val key = parts[0].lowercase()
              val value = parts[1]
              when (currentSection) {
                "Interface" -> interfaceParams[key] = value
                "Peer" -> peerParams[key] = value
              }
            }
          }
        }
      }
    }

    val endpoint = peerParams["endpoint"].orEmpty()
    val endpointParts = endpoint.split(":", limit = 2)
    val (server, serverPort) =
      if (endpointParts.size == 2) {
        endpointParts[0] to endpointParts[1]
      } else {
        endpoint to ""
      }

    return ConnectionProfile(
      protocol = Protocol.WireGuard,
      remarks = System.currentTimeMillis().toString(),
      secretKey = interfaceParams["privatekey"].orEmpty(),
      localAddress = interfaceParams["address"] ?: WIREGUARD_LOCAL_ADDRESS_V4,
      mtu = Utils.parseInt(interfaceParams["mtu"] ?: AppConfig.WIREGUARD_LOCAL_MTU),
      publicKey = peerParams["publickey"].orEmpty(),
      preSharedKey = peerParams["presharedkey"]?.nullIfBlank(),
      server = server,
      serverPort = serverPort,
      reserved = peerParams["reserved"] ?: "0,0,0",
    )
  }

  fun toOutbound(connectionProfile: ConnectionProfile): Outbound? {
    val outbound = V2rayConfigManager.createInitOutbound(Protocol.WireGuard)

    outbound?.settings?.let { wireguard ->
      wireguard.secretKey = connectionProfile.secretKey
      wireguard.address = (connectionProfile.localAddress ?: WIREGUARD_LOCAL_ADDRESS_V4).split(",")
      wireguard.peers?.firstOrNull()?.let { peer ->
        peer.publicKey = connectionProfile.publicKey.orEmpty()
        peer.preSharedKey = connectionProfile.preSharedKey?.nullIfBlank()
        peer.endpoint = Utils.getIpv6Address(connectionProfile.server) + ":${connectionProfile.serverPort}"
      }
      wireguard.mtu = connectionProfile.mtu
      wireguard.reserved =
        connectionProfile.reserved?.takeIf { it.isNotBlank() }?.split(",")?.filter { it.isNotBlank() }
          ?.map { it.trim().toInt() }
    }

    return outbound
  }

  fun toUri(config: ConnectionProfile): String {
    val dicQuery = HashMap<String, String>()

    dicQuery["publickey"] = config.publicKey.orEmpty()
    if (config.reserved != null) {
      dicQuery["reserved"] = config.reserved.removeWhiteSpace().orEmpty()
    }
    dicQuery["address"] = config.localAddress.removeWhiteSpace().orEmpty()
    if (config.mtu != null) {
      dicQuery["mtu"] = config.mtu.toString()
    }
    if (config.preSharedKey != null) {
      dicQuery["presharedkey"] = config.preSharedKey.removeWhiteSpace().orEmpty()
    }

    return toUri(config, config.secretKey, dicQuery)
  }
}
