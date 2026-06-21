package com.v2ray.ang.protocolstringsparsers

import android.util.Log
import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.ConnectionProfile
import com.v2ray.ang.dto.V2rayConfig.Outbound
import com.v2ray.ang.enums.NetworkType
import com.v2ray.ang.enums.Protocol
import com.v2ray.ang.extension.idnHost
import com.v2ray.ang.runtime.V2rayConfigManager
import com.v2ray.ang.util.Utils
import java.net.URI

object Shadowsocks : ProtocolParser() {
  fun parse(str: String): ConnectionProfile? {
    return parseSip002(str) ?: parseLegacy(str)
  }

  fun parseSip002(str: String): ConnectionProfile? {
    val uri = URI(Utils.fixIllegalUrl(str))
    if (uri.idnHost.isEmpty()) return null
    if (uri.port <= 0) return null
    if (uri.userInfo.isNullOrEmpty()) return null

    val userSplit =
      if (uri.userInfo.contains(":")) {
        uri.userInfo.split(":", limit = 2)
      } else {
        Utils.decode(uri.userInfo).split(":", limit = 2)
      }
    if (userSplit.size != 2) return null

    var config =
      ConnectionProfile(
        protocol = Protocol.ShadowSocks,
        remarks = Utils.decodeURIComponent(uri.fragment.orEmpty()).let { it.ifEmpty { "none" } },
        server = uri.idnHost,
        serverPort = uri.port.toString(),
        method = userSplit.first(),
        password = userSplit.last(),
        subscriptionId = uri.idnHost + uri.port.toString() + Protocol.ShadowSocks + userSplit.first(),
      )

    if (!uri.rawQuery.isNullOrEmpty()) {
      val queryParam = getQueryParam(uri)
      if (queryParam["plugin"]?.contains("obfs=http") == true) {
        val queryPairs = HashMap<String, String>()
        for (pair in queryParam["plugin"]?.split(";") ?: listOf()) {
          val idx = pair.split("=")
          if (idx.size == 2) {
            queryPairs[idx.first()] = idx.last()
          }
        }
        config =
          config.copy(
            network = NetworkType.TCP.type,
            headerType = "http",
            host = queryPairs["obfs-host"],
            path = queryPairs["path"],
          )
      }
    }

    return config
  }

  fun parseLegacy(str: String): ConnectionProfile? {
    var result = str.replace(Protocol.ShadowSocks.protocolScheme, "")
    val indexSplit = result.indexOf("#")
    val remarks =
      if (indexSplit > 0) {
        val rawRemark = result.substring(indexSplit + 1)
        result = result.substring(0, indexSplit)
        try {
          Utils.decodeURIComponent(rawRemark)
        } catch (illegal: IllegalArgumentException) {
          Log.e(AppConfig.TAG, "Failed to decode remarks in SS legacy URL", illegal)
          rawRemark
        }
      } else {
        "none"
      }

    val indexS = result.indexOf("@")
    result =
      if (indexS > 0) {
        Utils.decode(result.substring(0, indexS)) + result.substring(indexS)
      } else {
        Utils.decode(result)
      }

    val legacyPattern = "^(.+?):(.*)@(.+?):(\\d+?)/?$".toRegex()
    val match = legacyPattern.matchEntire(result) ?: return null

    return ConnectionProfile(
      protocol = Protocol.ShadowSocks,
      remarks = remarks,
      server = match.groupValues[3].removeSurrounding("[", "]"),
      serverPort = match.groupValues[4],
      password = match.groupValues[2],
      method = match.groupValues[1].lowercase(),
      subscriptionId = Protocol.ShadowSocks.value.toString() + remarks + match.groupValues[4] + match.groupValues[2],
    )
  }

  fun toUri(config: ConnectionProfile): String {
    val pw = "${config.method}:${config.password}"

    return toUri(config, Utils.encode(pw, true), null)
  }

  fun toOutbound(connectionProfile: ConnectionProfile): Outbound? {
    val outbound = V2rayConfigManager.createInitOutbound(Protocol.ShadowSocks)

    outbound?.settings?.servers?.first()?.let { server ->
      server.address = getServerAddress(connectionProfile)
      server.port = connectionProfile.serverPort.orEmpty().toInt()
      server.password = connectionProfile.password
      server.method = connectionProfile.method
    }

    val sni =
      outbound?.streamSettings?.let {
        V2rayConfigManager.populateTransportSettings(it, connectionProfile)
      }

    outbound?.streamSettings?.let {
      V2rayConfigManager.populateTlsSettings(it, connectionProfile, sni)
    }

    return outbound
  }
}
