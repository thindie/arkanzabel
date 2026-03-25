package com.v2ray.ang.protocolstringsparsers

import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.ConnectionProfile
import com.v2ray.ang.dto.V2rayConfig.Outbound
import com.v2ray.ang.dto.V2rayConfig.Outbound.StreamSettings.FinalMask
import com.v2ray.ang.enums.Protocol
import com.v2ray.ang.enums.NetworkType
import com.v2ray.ang.extension.idnHost
import com.v2ray.ang.extension.isNotNullEmpty
import com.v2ray.ang.extension.nullIfBlank
import com.v2ray.ang.runtime.KeyValueStorage
import com.v2ray.ang.runtime.V2rayConfigManager
import com.v2ray.ang.util.Utils
import java.net.URI

object Hysteria2 : ProtocolParser() {
  /**
   * Parses a Hysteria2 URI string into a ProfileItem object.
   *
   * @param str the Hysteria2 URI string to parse
   * @return the parsed ProfileItem object, or null if parsing fails
   */
  fun parse(str: String): ConnectionProfile? {
    var allowInsecure = KeyValueStorage.decodeSettingsBool(AppConfig.PREF_ALLOW_INSECURE, false)
    val config = ConnectionProfile(protocol = Protocol.Hysteria2)

    val uri = URI(Utils.fixIllegalUrl(str))
    config.remarks = Utils.decodeURIComponent(uri.fragment.orEmpty()).let { it.ifEmpty { "none" } }
    config.server = uri.idnHost
    config.serverPort = uri.port.toString()
    config.password = uri.userInfo
    config.security = AppConfig.TLS
    config.network = NetworkType.HYSTERIA.type

    if (!uri.rawQuery.isNullOrEmpty()) {
      val queryParam = getQueryParam(uri)

      getItemFormQuery(config, queryParam, allowInsecure)

      config.security = queryParam["security"] ?: AppConfig.TLS
      config.obfsPassword = queryParam["obfs-password"]
      config.portHopping = queryParam["mport"]
      if (config.portHopping.isNotNullEmpty()) {
        config.portHoppingInterval = queryParam["mportHopInt"]
      }
      config.pinnedCA256 = queryParam["pinSHA256"]

    }

    return config
  }

  /**
   * Converts a ProfileItem object to a URI string.
   *
   * @param config the ProfileItem object to convert
   * @return the converted URI string
   */
  fun toUri(config: ConnectionProfile): String {
    val dicQuery = HashMap<String, String>()

    config.security.let { if (it != null) dicQuery["security"] = it }
    config.sni?.nullIfBlank()?.let { dicQuery["sni"] = it }
    config.alpn?.nullIfBlank()?.let { dicQuery["alpn"] = it }
    dicQuery["insecure"] = if (config.insecure) "1" else "0"

    if (config.obfsPassword.isNotNullEmpty()) {
      dicQuery["obfs"] = "salamander"
      dicQuery["obfs-password"] = config.obfsPassword.orEmpty()
    }
    if (config.portHopping.isNotNullEmpty()) {
      dicQuery["mport"] = config.portHopping.orEmpty()
    }
    if (config.portHoppingInterval.isNotNullEmpty()) {
      dicQuery["mportHopInt"] = config.portHoppingInterval.orEmpty()
    }
    if (config.pinnedCA256.isNotNullEmpty()) {
      dicQuery["pinSHA256"] = config.pinnedCA256.orEmpty()
    }

    return toUri(config, config.password, dicQuery)
  }

  /**
   * Converts a ProfileItem object to an Outbound object.
   *
   * @param connectionProfile the ProfileItem object to convert
   * @return the converted Outbound object, or null if conversion fails
   */
  fun toOutbound(connectionProfile: ConnectionProfile): Outbound? {
    val outbound = V2rayConfigManager.createInitOutbound(Protocol.Hysteria2) ?: return null
    connectionProfile.network = NetworkType.HYSTERIA.type
    connectionProfile.alpn = "h3"

    outbound.settings?.let { server ->
      server.address = getServerAddress(connectionProfile)
      server.port = connectionProfile.serverPort.orEmpty().toInt()
      server.version = 2
    }

    val sni = outbound.streamSettings?.let {
      V2rayConfigManager.populateTransportSettings(it, connectionProfile)
    }

    outbound.streamSettings?.let {
      V2rayConfigManager.populateTlsSettings(it, connectionProfile, sni)
    }

    if (connectionProfile.obfsPassword.isNotNullEmpty()) {
      outbound.streamSettings?.finalmask = FinalMask(
        udp = listOf(
          FinalMask.Mask(
            type = "salamander",
            settings = FinalMask.Mask.MaskSettings(
              password = connectionProfile.obfsPassword
            )
          )
        )
      )
    }
    return outbound
  }
}