package com.v2ray.ang.protocolstringsparsers

import android.util.Log
import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.ConnectionProfile
import com.v2ray.ang.dto.V2rayConfig.OutboundBean
import com.v2ray.ang.enums.Protocol
import com.v2ray.ang.enums.NetworkType
import com.v2ray.ang.extension.idnHost
import com.v2ray.ang.runtime.V2rayConfigManager
import com.v2ray.ang.util.Utils
import java.net.URI

object Shadowsocks : ProtocolParser() {
  /**
   * Parses a Shadowsocks URI string into a ProfileItem object.
   *
   * @param str the Shadowsocks URI string to parse
   * @return the parsed ProfileItem object, or null if parsing fails
   */
  fun parse(str: String): ConnectionProfile? {
    return parseSip002(str) ?: parseLegacy(str)
  }

  /**
   * Parses a SIP002 Shadowsocks URI string into a ProfileItem object.
   *
   * @param str the SIP002 Shadowsocks URI string to parse
   * @return the parsed ProfileItem object, or null if parsing fails
   */
  fun parseSip002(str: String): ConnectionProfile? {
    val config = ConnectionProfile.create(Protocol.ShadowSocks)

    val uri = URI(Utils.fixIllegalUrl(str))
    if (uri.idnHost.isEmpty()) return null
    if (uri.port <= 0) return null
    if (uri.userInfo.isNullOrEmpty()) return null

    config.remarks = Utils.decodeURIComponent(uri.fragment.orEmpty()).let { it.ifEmpty { "none" } }
    config.server = uri.idnHost
    config.serverPort = uri.port.toString()

    val result = if (uri.userInfo.contains(":")) {
      uri.userInfo.split(":", limit = 2)
    } else {
      Utils.decode(uri.userInfo).split(":", limit = 2)
    }
    if (result.count() == 2) {
      config.method = result.first()
      config.password = result.last()
    }

    if (!uri.rawQuery.isNullOrEmpty()) {
      val queryParam = getQueryParam(uri)
      if (queryParam["plugin"]?.contains("obfs=http") == true) {
        val queryPairs = HashMap<String, String>()
        for (pair in queryParam["plugin"]?.split(";") ?: listOf()) {
          val idx = pair.split("=")
          if (idx.count() == 2) {
            queryPairs.put(idx.first(), idx.last())
          }
        }
        config.network = NetworkType.TCP.type
        config.headerType = "http"
        config.host = queryPairs["obfs-host"]
        config.path = queryPairs["path"]
      }
    }

    return config
  }

  /**
   * Parses a legacy Shadowsocks URI string into a ProfileItem object.
   *
   * @param str the legacy Shadowsocks URI string to parse
   * @return the parsed ProfileItem object, or null if parsing fails
   */
  fun parseLegacy(str: String): ConnectionProfile? {
    val config = ConnectionProfile.create(Protocol.ShadowSocks)
    var result = str.replace(Protocol.ShadowSocks.protocolScheme, "")
    val indexSplit = result.indexOf("#")
    if (indexSplit > 0) {
      try {
        config.remarks =
          Utils.decodeURIComponent(result.substring(indexSplit + 1, result.length))
      } catch (e: Exception) {
        Log.e(AppConfig.TAG, "Failed to decode remarks in SS legacy URL", e)
      }

      result = result.substring(0, indexSplit)
    }

    //part decode
    val indexS = result.indexOf("@")
    result = if (indexS > 0) {
      Utils.decode(result.substring(0, indexS)) + result.substring(
        indexS,
        result.length
      )
    } else {
      Utils.decode(result)
    }

    val legacyPattern = "^(.+?):(.*)@(.+?):(\\d+?)/?$".toRegex()
    val match = legacyPattern.matchEntire(result) ?: return null

    config.server = match.groupValues[3].removeSurrounding("[", "]")
    config.serverPort = match.groupValues[4]
    config.password = match.groupValues[2]
    config.method = match.groupValues[1].lowercase()

    return config
  }

  fun toUri(config: ConnectionProfile): String {
    val pw = "${config.method}:${config.password}"

    return toUri(config, Utils.encode(pw, true), null)
  }

  fun toOutbound(connectionProfile: ConnectionProfile): OutboundBean? {
    val outboundBean = V2rayConfigManager.createInitOutbound(Protocol.ShadowSocks)

    outboundBean?.settings?.servers?.first()?.let { server ->
      server.address = getServerAddress(connectionProfile)
      server.port = connectionProfile.serverPort.orEmpty().toInt()
      server.password = connectionProfile.password
      server.method = connectionProfile.method
    }

    val sni = outboundBean?.streamSettings?.let {
      V2rayConfigManager.populateTransportSettings(it, connectionProfile)
    }

    outboundBean?.streamSettings?.let {
      V2rayConfigManager.populateTlsSettings(it, connectionProfile, sni)
    }

    return outboundBean
  }
}