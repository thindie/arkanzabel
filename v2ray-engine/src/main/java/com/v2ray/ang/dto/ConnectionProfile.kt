package com.v2ray.ang.dto

import com.google.errorprone.annotations.Immutable
import com.v2ray.ang.AppConfig.LOOPBACK
import com.v2ray.ang.AppConfig.PORT_SOCKS
import com.v2ray.ang.AppConfig.TAG_BLOCKED
import com.v2ray.ang.AppConfig.TAG_DIRECT
import com.v2ray.ang.AppConfig.TAG_PROXY
import com.v2ray.ang.enums.Protocol
import com.v2ray.ang.util.Utils

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
  val network: String? = null,
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
  val security: String? = null,
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
) {
  fun getAllOutboundTags(): List<String> {
    return listOf(TAG_PROXY, TAG_DIRECT, TAG_BLOCKED)
  }

  fun getServerAddressAndPort(): String {
    if (server.isNullOrEmpty() && protocol == Protocol.Custom) {
      return "$LOOPBACK:$PORT_SOCKS"
    }
    return Utils.getIpv6Address(server) + ":" + serverPort
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ConnectionProfile) return false

    return (
      this.server == other.server &&
        this.serverPort == other.serverPort &&
        this.password == other.password &&
        this.method == other.method &&
        this.flow == other.flow &&
        this.username == other.username &&

        this.network == other.network &&
        this.headerType == other.headerType &&
        this.host == other.host &&
        this.path == other.path &&
        this.seed == other.seed &&
        this.quicSecurity == other.quicSecurity &&
        this.quicKey == other.quicKey &&
        this.mode == other.mode &&
        this.serviceName == other.serviceName &&
        this.authority == other.authority &&
        this.xhttpMode == other.xhttpMode &&
        this.xhttpExtra == other.xhttpExtra &&

        this.security == other.security &&
        this.sni == other.sni &&
        this.alpn == other.alpn &&
        this.fingerPrint == other.fingerPrint &&
        this.insecure == other.insecure &&
        this.echConfigList == other.echConfigList &&
        this.echForceQuery == other.echForceQuery &&
        this.publicKey == other.publicKey &&
        this.shortId == other.shortId &&
        this.spiderX == other.spiderX &&
        this.mldsa65Verify == other.mldsa65Verify &&

        this.secretKey == other.secretKey &&
        this.preSharedKey == other.preSharedKey &&
        this.localAddress == other.localAddress &&
        this.reserved == other.reserved &&
        this.mtu == other.mtu &&

        this.obfsPassword == other.obfsPassword &&
        this.portHopping == other.portHopping &&
        this.portHoppingInterval == other.portHoppingInterval &&
        this.pinSHA256 == other.pinSHA256 &&
        this.pinnedCA256 == other.pinnedCA256 &&
        this.bandwidthDown == other.bandwidthDown &&
        this.bandwidthUp == other.bandwidthUp &&
        this.policyGroupType == other.policyGroupType &&
        this.policyGroupSubscriptionId == other.policyGroupSubscriptionId &&
        this.policyGroupFilter == other.policyGroupFilter
    )
  }

  override fun hashCode(): Int {
    var result = server?.hashCode() ?: 0
    result = 31 * result + (serverPort?.hashCode() ?: 0)
    result = 31 * result + (password?.hashCode() ?: 0)
    result = 31 * result + (method?.hashCode() ?: 0)
    result = 31 * result + (flow?.hashCode() ?: 0)
    result = 31 * result + (username?.hashCode() ?: 0)
    result = 31 * result + (network?.hashCode() ?: 0)
    result = 31 * result + (headerType?.hashCode() ?: 0)
    result = 31 * result + (host?.hashCode() ?: 0)
    result = 31 * result + (path?.hashCode() ?: 0)
    result = 31 * result + (seed?.hashCode() ?: 0)
    result = 31 * result + (quicSecurity?.hashCode() ?: 0)
    result = 31 * result + (quicKey?.hashCode() ?: 0)
    result = 31 * result + (mode?.hashCode() ?: 0)
    result = 31 * result + (serviceName?.hashCode() ?: 0)
    result = 31 * result + (authority?.hashCode() ?: 0)
    result = 31 * result + (xhttpMode?.hashCode() ?: 0)
    result = 31 * result + (xhttpExtra?.hashCode() ?: 0)
    result = 31 * result + (security?.hashCode() ?: 0)
    result = 31 * result + (sni?.hashCode() ?: 0)
    result = 31 * result + (alpn?.hashCode() ?: 0)
    result = 31 * result + (fingerPrint?.hashCode() ?: 0)
    result = 31 * result + insecure.hashCode()
    result = 31 * result + (echConfigList?.hashCode() ?: 0)
    result = 31 * result + (echForceQuery?.hashCode() ?: 0)
    result = 31 * result + (publicKey?.hashCode() ?: 0)
    result = 31 * result + (shortId?.hashCode() ?: 0)
    result = 31 * result + (spiderX?.hashCode() ?: 0)
    result = 31 * result + (mldsa65Verify?.hashCode() ?: 0)
    result = 31 * result + (secretKey?.hashCode() ?: 0)
    result = 31 * result + (preSharedKey?.hashCode() ?: 0)
    result = 31 * result + (localAddress?.hashCode() ?: 0)
    result = 31 * result + (reserved?.hashCode() ?: 0)
    result = 31 * result + (mtu ?: 0)
    result = 31 * result + (obfsPassword?.hashCode() ?: 0)
    result = 31 * result + (portHopping?.hashCode() ?: 0)
    result = 31 * result + (portHoppingInterval?.hashCode() ?: 0)
    result = 31 * result + (pinSHA256?.hashCode() ?: 0)
    result = 31 * result + (pinnedCA256?.hashCode() ?: 0)
    result = 31 * result + (bandwidthDown?.hashCode() ?: 0)
    result = 31 * result + (bandwidthUp?.hashCode() ?: 0)
    result = 31 * result + (policyGroupType?.hashCode() ?: 0)
    result = 31 * result + (policyGroupSubscriptionId?.hashCode() ?: 0)
    result = 31 * result + (policyGroupFilter?.hashCode() ?: 0)
    return result
  }
}
