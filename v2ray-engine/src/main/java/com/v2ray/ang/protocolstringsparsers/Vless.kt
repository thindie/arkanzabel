package com.v2ray.ang.protocolstringsparsers

import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.ConnectionProfile
import com.v2ray.ang.dto.V2rayConfig.Outbound
import com.v2ray.ang.enums.Protocol
import com.v2ray.ang.extension.idnHost
import com.v2ray.ang.runtime.KeyValueStorage
import com.v2ray.ang.runtime.V2rayConfigManager
import com.v2ray.ang.util.Utils
import java.net.URI

object Vless : ProtocolParser() {

  private val realityPublicKeyFromRawRegexes =
    listOf(
      Regex("""(?i)(?:^|[?&])pbk=([^&#]+)"""),
      Regex("""(?i)(?:^|[?&])publicKey=([^&#]+)"""),
      Regex("""(?i)(?:^|[?&])public_key=([^&#]+)"""),
      Regex("""(?i)(?:^|[?&])public-key=([^&#]+)"""),
    )

  private fun mergeRealityPublicKeyFromRawLine(config: ConnectionProfile, rawLine: String): ConnectionProfile {
    if (!config.publicKey.isNullOrBlank()) return config
    val chunks =
      buildList {
        rawLine.substringAfter("?", "").substringBefore("#").trim().let { if (it.isNotEmpty()) add(it) }
        rawLine.substringAfter("#", "").trim().let {
          if (it.contains('=')) add(it)
        }
      }
    for (chunk in chunks) {
      for (pattern in realityPublicKeyFromRawRegexes) {
        val match = pattern.find(chunk) ?: continue
        val encoded = match.groupValues[1].trim()
        if (encoded.isEmpty()) continue
        val decoded = Utils.decodeURIComponent(encoded)
        if (decoded.isNotBlank()) {
          return config.copy(publicKey = decoded)
        }
      }
    }
    return config
  }

  fun parse(str: String): ConnectionProfile? {
    val allowInsecure = KeyValueStorage.decodeSettingsBool(AppConfig.PREF_ALLOW_INSECURE, false)

    val uri = URI(Utils.fixIllegalUrl(str))
    if (uri.rawQuery.isNullOrEmpty()) return null
    val queryParam = getQueryParam(uri)

    val base =
      ConnectionProfile(
        protocol = Protocol.Vless,
        remarks = Utils.decodeURIComponent(uri.fragment.orEmpty()).let { it.ifEmpty { "none" } },
        server = uri.idnHost,
        serverPort = uri.port.toString(),
        password = uri.userInfo,
        method = queryParam["encryption"] ?: "none",
        subscriptionId = uri.idnHost + uri.port.toString() + uri.userInfo + Protocol.Vless
      )

    val withQuery = getItemFormQuery(base, queryParam, allowInsecure)
    return mergeRealityPublicKeyFromRawLine(withQuery, str)
  }

  fun toUri(config: ConnectionProfile): String {
    val dicQuery = getQueryDic(config)
    dicQuery["encryption"] = config.method ?: "none"

    return toUri(config, config.password, dicQuery)
  }

  fun toOutbound(connectionProfile: ConnectionProfile): Outbound? {
    if (connectionProfile.security == AppConfig.REALITY && connectionProfile.publicKey.isNullOrBlank()) {
      return null
    }
    val outbound = V2rayConfigManager.createInitOutbound(Protocol.Vless)

    outbound?.settings?.vnext?.first()?.let { vnext ->
      vnext.address = getServerAddress(connectionProfile)
      vnext.port = connectionProfile.serverPort.orEmpty().toInt()
      vnext.users[0].id = connectionProfile.password.orEmpty()
      vnext.users[0].encryption = connectionProfile.method
      vnext.users[0].flow = connectionProfile.flow
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
