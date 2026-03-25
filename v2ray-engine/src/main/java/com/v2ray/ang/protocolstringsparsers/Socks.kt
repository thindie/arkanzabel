package com.v2ray.ang.protocolstringsparsers

import com.v2ray.ang.dto.ConnectionProfile
import com.v2ray.ang.dto.V2rayConfig.Outbound
import com.v2ray.ang.enums.Protocol
import com.v2ray.ang.extension.idnHost
import com.v2ray.ang.extension.isNotNullEmpty
import com.v2ray.ang.runtime.V2rayConfigManager
import com.v2ray.ang.util.Utils
import java.net.URI

object Socks : ProtocolParser() {
  fun parse(str: String): ConnectionProfile? {
    val config = ConnectionProfile.create(Protocol.Socks)

    val uri = URI(Utils.fixIllegalUrl(str))
    if (uri.idnHost.isEmpty()) return null
    if (uri.port <= 0) return null

    config.remarks = Utils.decodeURIComponent(uri.fragment.orEmpty()).let { it.ifEmpty { "none" } }
    config.server = uri.idnHost
    config.serverPort = uri.port.toString()

    if (uri.userInfo?.isEmpty() == false) {
      val result = Utils.decode(uri.userInfo).split(":", limit = 2)
      if (result.count() == 2) {
        config.username = result.first()
        config.password = result.last()
      }
    }

    return config
  }

  fun toUri(config: ConnectionProfile): String {
    val pw =
      if (config.username.isNotNullEmpty())
        "${config.username}:${config.password}"
      else
        ":"

    return toUri(config, Utils.encode(pw, true), null)
  }


  fun toOutbound(connectionProfile: ConnectionProfile): Outbound? {
    val outbound = V2rayConfigManager.createInitOutbound(Protocol.Socks)

    outbound?.settings?.servers?.first()?.let { server ->
      server.address = getServerAddress(connectionProfile)
      server.port = connectionProfile.serverPort.orEmpty().toInt()
      if (connectionProfile.username.isNotNullEmpty()) {
        val socksUsers = Outbound.OutSettings.Servers.SocksUsers()
        socksUsers.user = connectionProfile.username.orEmpty()
        socksUsers.pass = connectionProfile.password.orEmpty()
        server.users = listOf(socksUsers)
      }
    }

    return outbound
  }
}