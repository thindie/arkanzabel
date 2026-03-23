package com.v2ray.ang.protocolstringsparsers

import com.v2ray.ang.dto.ProfileItem
import com.v2ray.ang.dto.V2rayConfig.OutboundBean
import com.v2ray.ang.enums.Protocol
import com.v2ray.ang.extension.idnHost
import com.v2ray.ang.extension.isNotNullEmpty
import com.v2ray.ang.handler.V2rayConfigManager
import com.v2ray.ang.util.Utils
import java.net.URI

object Socks : ProtocolParser() {
  fun parse(str: String): ProfileItem? {
    val config = ProfileItem.create(Protocol.Socks)

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

  fun toUri(config: ProfileItem): String {
    val pw =
      if (config.username.isNotNullEmpty())
        "${config.username}:${config.password}"
      else
        ":"

    return toUri(config, Utils.encode(pw, true), null)
  }


  fun toOutbound(profileItem: ProfileItem): OutboundBean? {
    val outboundBean = V2rayConfigManager.createInitOutbound(Protocol.Socks)

    outboundBean?.settings?.servers?.first()?.let { server ->
      server.address = getServerAddress(profileItem)
      server.port = profileItem.serverPort.orEmpty().toInt()
      if (profileItem.username.isNotNullEmpty()) {
        val socksUsersBean = OutboundBean.OutSettingsBean.ServersBean.SocksUsersBean()
        socksUsersBean.user = profileItem.username.orEmpty()
        socksUsersBean.pass = profileItem.password.orEmpty()
        server.users = listOf(socksUsersBean)
      }
    }

    return outboundBean
  }
}