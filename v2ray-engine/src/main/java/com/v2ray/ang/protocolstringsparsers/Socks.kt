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
    val uri = URI(Utils.fixIllegalUrl(str))
    if (uri.idnHost.isEmpty()) return null
    if (uri.port <= 0) return null

    val (username, password) =
      if (uri.userInfo?.isEmpty() == false) {
        val parts = Utils.decode(uri.userInfo).split(":", limit = 2)
        if (parts.size == 2) parts[0] to parts[1] else null to null
      } else {
        null to null
      }

    return ConnectionProfile(
      protocol = Protocol.Socks,
      remarks = Utils.decodeURIComponent(uri.fragment.orEmpty()).let { it.ifEmpty { "none" } },
      server = uri.idnHost,
      serverPort = uri.port.toString(),
      username = username,
      password = password,
      subscriptionId = uri.userInfo + uri.idnHost + username
    )
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
