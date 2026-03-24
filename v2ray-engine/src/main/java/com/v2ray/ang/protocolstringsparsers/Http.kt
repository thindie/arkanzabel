package com.v2ray.ang.protocolstringsparsers

import com.v2ray.ang.dto.ConnectionProfile
import com.v2ray.ang.dto.V2rayConfig.OutboundBean
import com.v2ray.ang.enums.Protocol
import com.v2ray.ang.extension.isNotNullEmpty
import com.v2ray.ang.runtime.V2rayConfigManager

object Http : ProtocolParser() {
  /**
   * Converts a ProfileItem object to an OutboundBean object.
   *
   * @param connectionProfile the СonnectionProfile object to convert
   * @return the converted OutboundBean object, or null if conversion fails
   */
  fun toOutbound(connectionProfile: ConnectionProfile): OutboundBean? {
    val outboundBean = V2rayConfigManager.createInitOutbound(Protocol.Http)

    outboundBean?.settings?.servers?.first()?.let { server ->
      server.address = getServerAddress(connectionProfile)
      server.port = connectionProfile.serverPort.orEmpty().toInt()
      if (connectionProfile.username.isNotNullEmpty()) {
        val socksUsersBean = OutboundBean.OutSettingsBean.ServersBean.SocksUsersBean()
        socksUsersBean.user = connectionProfile.username.orEmpty()
        socksUsersBean.pass = connectionProfile.password.orEmpty()
        server.users = listOf(socksUsersBean)
      }
    }

    return outboundBean
  }
}