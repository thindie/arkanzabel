package com.v2ray.ang.protocolstringsparsers

import com.v2ray.ang.dto.ConnectionProfile
import com.v2ray.ang.dto.V2rayConfig
import com.v2ray.ang.enums.Protocol
import com.v2ray.ang.util.JsonUtil

object Custom : ProtocolParser() {
  /**
   * Parses a JSON string into a ProfileItem object.
   *
   * @param str the JSON string to parse
   * @return the parsed ProfileItem object, or null if parsing fails
   */
  fun parse(str: String): ConnectionProfile? {
    val config = ConnectionProfile.create(Protocol.Custom)

    val fullConfig = JsonUtil.fromJson(str, V2rayConfig::class.java)
    val outbound = fullConfig?.getProxyOutbound()

    config.remarks = fullConfig?.remarks ?: System.currentTimeMillis().toString()
    config.server = outbound?.getServerAddress()
    config.serverPort = outbound?.getServerPort().toString()

    return config
  }
}