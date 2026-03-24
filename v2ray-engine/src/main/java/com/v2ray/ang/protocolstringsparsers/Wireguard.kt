package com.v2ray.ang.protocolstringsparsers

import com.v2ray.ang.AppConfig
import com.v2ray.ang.AppConfig.WIREGUARD_LOCAL_ADDRESS_V4
import com.v2ray.ang.dto.ConnectionProfile
import com.v2ray.ang.dto.V2rayConfig.OutboundBean
import com.v2ray.ang.enums.Protocol
import com.v2ray.ang.extension.idnHost
import com.v2ray.ang.extension.nullIfBlank
import com.v2ray.ang.extension.removeWhiteSpace
import com.v2ray.ang.runtime.V2rayConfigManager
import com.v2ray.ang.util.Utils
import java.net.URI

object Wireguard : ProtocolParser() {
  /**
   * Parses a URI string into a ProfileItem object.
   *
   * @param str the URI string to parse
   * @return the parsed ProfileItem object, or null if parsing fails
   */
  fun parse(str: String): ConnectionProfile? {
    val config = ConnectionProfile.create(Protocol.WireGuard)

    val uri = URI(Utils.fixIllegalUrl(str))
    if (uri.rawQuery.isNullOrEmpty()) return null
    val queryParam = getQueryParam(uri)

    config.remarks = Utils.decodeURIComponent(uri.fragment.orEmpty()).let { it.ifEmpty { "none" } }
    config.server = uri.idnHost
    config.serverPort = uri.port.toString()

    config.secretKey = uri.userInfo.orEmpty()
    config.localAddress = queryParam["address"] ?: WIREGUARD_LOCAL_ADDRESS_V4
    config.publicKey = queryParam["publickey"].orEmpty()
    config.preSharedKey = queryParam["presharedkey"]?.nullIfBlank()
    config.mtu = Utils.parseInt(queryParam["mtu"] ?: AppConfig.WIREGUARD_LOCAL_MTU)
    config.reserved = queryParam["reserved"] ?: "0,0,0"

    return config
  }

  fun parseWireguardConfFile(str: String): ConnectionProfile? {
    val config = ConnectionProfile.create(Protocol.WireGuard)

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

    config.secretKey = interfaceParams["privatekey"].orEmpty()
    config.remarks = System.currentTimeMillis().toString()
    config.localAddress = interfaceParams["address"] ?: WIREGUARD_LOCAL_ADDRESS_V4
    config.mtu = Utils.parseInt(interfaceParams["mtu"] ?: AppConfig.WIREGUARD_LOCAL_MTU)
    config.publicKey = peerParams["publickey"].orEmpty()
    config.preSharedKey = peerParams["presharedkey"]?.nullIfBlank()
    val endpoint = peerParams["endpoint"].orEmpty()
    val endpointParts = endpoint.split(":", limit = 2)
    if (endpointParts.size == 2) {
      config.server = endpointParts[0]
      config.serverPort = endpointParts[1]
    } else {
      config.server = endpoint
      config.serverPort = ""
    }
    config.reserved = peerParams["reserved"] ?: "0,0,0"

    return config
  }

  fun toOutbound(connectionProfile: ConnectionProfile): OutboundBean? {
    val outboundBean = V2rayConfigManager.createInitOutbound(Protocol.WireGuard)

    outboundBean?.settings?.let { wireguard ->
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

    return outboundBean
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
