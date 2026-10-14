package com.v2ray.ang.dto

import com.google.errorprone.annotations.Immutable
import com.v2ray.ang.AppConfig.TAG_BLOCKED
import com.v2ray.ang.AppConfig.TAG_DIRECT
import com.v2ray.ang.AppConfig.TAG_PROXY
import com.v2ray.ang.enums.NetworkType
import com.v2ray.ang.enums.Protocol
import com.v2ray.ang.enums.Security

@Immutable
data class ConnectionProfile(
  val configVersion: Int = 4,
  val protocol: Protocol,
  val subscriptionId: String,
  val addedTime: Long = System.currentTimeMillis(),
  val remarks: String = "",
  val description: String? = null,
  val server: String? = null,
  val serverPort: String? = null,
  val password: String? = null,
  val method: String? = null,
  val flow: String? = null,
  val username: String? = null,
  val network: NetworkType? = null,
  val headerType: String? = null,
  val host: String? = null,
  val path: String? = null,
  val seed: String? = null,
  val quicSecurity: String? = null,
  val quicKey: String? = null,
  val mode: String? = null,
  val serviceName: String? = null,
  val authority: String? = null,
  val xhttpMode: String? = null,
  val xhttpExtra: String? = null,
  val security: Security? = null,
  val sni: String? = null,
  val alpn: String? = null,
  val fingerPrint: String? = null,
  val insecure: Boolean = false,
  val echConfigList: String? = null,
  val echForceQuery: String? = null,
  val pinnedCA256: String? = null,
  val publicKey: String? = null,
  val shortId: String? = null,
  val spiderX: String? = null,
  val mldsa65Verify: String? = null,
  val secretKey: String? = null,
  val preSharedKey: String? = null,
  val localAddress: String? = null,
  val reserved: String? = null,
  val mtu: Int? = null,
  val obfsPassword: String? = null,
  val portHopping: String? = null,
  val portHoppingInterval: String? = null,
  val pinSHA256: String? = null,
  val bandwidthDown: String? = null,
  val bandwidthUp: String? = null,
  val policyGroupType: String? = null,
  val policyGroupSubscriptionId: String? = null,
  val policyGroupFilter: String? = null,
  val delay: Long? = null,
) {
  fun getAllOutboundTags(): List<String> {
    return listOf(TAG_PROXY, TAG_DIRECT, TAG_BLOCKED)
  }
}
