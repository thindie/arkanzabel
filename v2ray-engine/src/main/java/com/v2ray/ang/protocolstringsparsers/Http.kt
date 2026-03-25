package com.v2ray.ang.protocolstringsparsers

import com.v2ray.ang.dto.ConnectionProfile
import com.v2ray.ang.dto.V2rayConfig.Outbound
import com.v2ray.ang.enums.Protocol
import com.v2ray.ang.extension.isNotNullEmpty
import com.v2ray.ang.runtime.V2rayConfigManager

object Http : ProtocolParser() {
  /**
   * Converts a ProfileItem object to an Outbound object.
   *
   * @param connectionProfile the СonnectionProfile object to convert
   * @return the converted Outbound object, or null if conversion fails
   */
  fun toOutbound(connectionProfile: ConnectionProfile): Outbound? {
    val outbound = V2rayConfigManager.createInitOutbound(Protocol.Http)

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