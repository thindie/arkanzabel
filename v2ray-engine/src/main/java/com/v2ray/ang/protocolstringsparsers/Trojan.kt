package com.v2ray.ang.protocolstringsparsers

import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.ConnectionProfile
import com.v2ray.ang.dto.V2rayConfig.Outbound
import com.v2ray.ang.enums.NetworkType
import com.v2ray.ang.enums.Protocol
import com.v2ray.ang.extension.idnHost
import com.v2ray.ang.runtime.KeyValueStorage
import com.v2ray.ang.runtime.V2rayConfigManager
import com.v2ray.ang.util.Utils
import java.net.URI

object Trojan : ProtocolParser() {
  fun parse(str: String): ConnectionProfile? {
    val allowInsecure = KeyValueStorage.decodeSettingsBool(AppConfig.PREF_ALLOW_INSECURE, false)

    val uri = URI(Utils.fixIllegalUrl(str))
    val base =
      ConnectionProfile(
        protocol = Protocol.Trojan,
        remarks = Utils.decodeURIComponent(uri.fragment.orEmpty()).let { it.ifEmpty { "none" } },
        server = uri.idnHost,
        serverPort = uri.port.toString(),
        password = uri.userInfo,
        subscriptionId = uri.idnHost + uri.port.toString() + uri.userInfo,
      )

    return if (uri.rawQuery.isNullOrEmpty()) {
      base.copy(
        network = NetworkType.TCP.type,
        security = AppConfig.TLS,
        insecure = allowInsecure,
      )
    } else {
      val queryParam = getQueryParam(uri)
      getItemFormQuery(base, queryParam, allowInsecure).copy(
        security = queryParam["security"] ?: AppConfig.TLS,
      )
    }
  }

  fun toUri(config: ConnectionProfile): String {
    val dicQuery = getQueryDic(config)

    return toUri(config, config.password, dicQuery)
  }

  fun toOutbound(connectionProfile: ConnectionProfile): Outbound? {
    val outbound = V2rayConfigManager.createInitOutbound(Protocol.Trojan)

    outbound?.settings?.servers?.first()?.let { server ->
      server.address = getServerAddress(connectionProfile)
      server.port = connectionProfile.serverPort.orEmpty().toInt()
      server.password = connectionProfile.password
      server.flow = connectionProfile.flow
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
