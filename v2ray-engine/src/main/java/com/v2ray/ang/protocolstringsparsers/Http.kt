package com.v2ray.ang.protocolstringsparsers

import com.v2ray.ang.dto.ConnectionProfile
import com.v2ray.ang.dto.V2rayConfig.Outbound
import com.v2ray.ang.enums.Protocol
import com.v2ray.ang.extension.isNotNullEmpty
import com.v2ray.ang.runtime.V2rayConfigManager

object Http : ProtocolParser() {
  /**
   * Converts a [ConnectionProfile] object to an [Outbound] object.
   *
   * @param connectionProfile the [ConnectionProfile] object to convert
   * @return the converted [Outbound] object, or null if conversion fails
   */
  fun toOutbound(connectionProfile: ConnectionProfile): Outbound? {
    val outbound = V2rayConfigManager.createInitOutbound(Protocol.Http)
    if (outbound == null) return null

    val outboundSettings = outbound.settings ?: return outbound
    val servers = outboundSettings.servers ?: return outbound
    if (servers.isEmpty()) return outbound

    val firstServer =
      servers.first().copy(
        address = getServerAddress(connectionProfile),
        port = connectionProfile.serverPort.orEmpty().toInt(),
        users =
          listOfNotNull(
            if (connectionProfile.username.isNotNullEmpty()) {
              Outbound.OutSettings.Servers.SocksUsers(
                user = connectionProfile.username.orEmpty(),
                pass = connectionProfile.password.orEmpty(),
              )
            } else {
              null
            },
          ),
      )

    val updatedSettings =
      outboundSettings.copy(
        servers = listOf(firstServer) + servers.drop(1),
      )

    return outbound.copy(settings = updatedSettings)
  }
}
