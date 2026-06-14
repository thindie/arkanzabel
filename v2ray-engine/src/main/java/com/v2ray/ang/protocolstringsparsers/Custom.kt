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
    val config = ConnectionProfile(protocol = Protocol.Custom, subscriptionId = "")

    val fullConfig = JsonUtil.fromJson(str, V2rayConfig::class.java)
    val outbound = fullConfig?.getProxyOutbound()

    return config.copy(
      remarks = fullConfig?.remarks ?: System.currentTimeMillis().toString(),
      server = outbound?.getServerAddress(),
      serverPort = outbound?.getServerPort().toString(),
      subscriptionId = fullConfig?.remarks.toString() + outbound?.getServerAddress() + outbound?.getServerPort().toString()
    )
  }
}