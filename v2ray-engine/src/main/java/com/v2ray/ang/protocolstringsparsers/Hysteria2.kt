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
   * Parses a Hysteria2 URI string into a [ConnectionProfile], or null if parsing fails.
   */
  fun parse(str: String): ConnectionProfile? {
    val allowInsecure = KeyValueStorage.decodeSettingsBool(AppConfig.PREF_ALLOW_INSECURE, false)
    val uri = URI(Utils.fixIllegalUrl(str))
    var result =
      ConnectionProfile(
        protocol = Protocol.Hysteria2,
        remarks = Utils.decodeURIComponent(uri.fragment.orEmpty()).ifBlank { "none" },
        server = uri.idnHost,
        serverPort = uri.port.toString(),
        password = uri.userInfo,
        security = AppConfig.TLS,
        network = NetworkType.HYSTERIA.type,
        subscriptionId = uri.idnHost + uri.port.toString() + uri.userInfo
      )

    if (!uri.rawQuery.isNullOrEmpty()) {
      val queryParam = getQueryParam(uri)
      result = getItemFormQuery(result, queryParam, allowInsecure)
      result =
        result.copy(
          security = queryParam["security"] ?: AppConfig.TLS,
          obfsPassword = queryParam["obfs-password"],
          portHopping = queryParam["mport"],
          portHoppingInterval = queryParam["mportHopInt"]?.ifBlank { null },
          pinnedCA256 = queryParam["pinSHA256"],
        )
    }
    return result
  }

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

  fun toOutbound(connectionProfile: ConnectionProfile): Outbound? {
    val outbound = V2rayConfigManager.createInitOutbound(Protocol.Hysteria2) ?: return null
    val profile =
      connectionProfile.copy(
        network = NetworkType.HYSTERIA.type,
        alpn = "h3",
      )

    outbound.settings?.let { server ->
      server.address = getServerAddress(profile)
      server.port = profile.serverPort.orEmpty().toInt()
      server.version = 2
    }

    val sni =
      outbound.streamSettings?.let {
        V2rayConfigManager.populateTransportSettings(it, profile)
      }

    outbound.streamSettings?.let {
      V2rayConfigManager.populateTlsSettings(it, profile, sni)
    }

    if (profile.obfsPassword.isNotNullEmpty()) {
      outbound.streamSettings?.finalmask =
        FinalMask(
          udp =
            listOf(
              FinalMask.Mask(
                type = "salamander",
                settings =
                  FinalMask.Mask.MaskSettings(
                    password = profile.obfsPassword,
                  ),
              ),
            ),
        )
    }
    return outbound
  }
}
