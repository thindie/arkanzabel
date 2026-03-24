package com.v2ray.ang.handler

import android.util.Log
import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.ConnectionProfile
import com.v2ray.ang.dto.V2rayConfig
import com.v2ray.ang.dto.V2rayConfig.OutboundBean
import com.v2ray.ang.dto.V2rayConfig.OutboundBean.OutSettingsBean
import com.v2ray.ang.dto.V2rayConfig.OutboundBean.StreamSettingsBean
import com.v2ray.ang.error.OutboundConfigError
import com.v2ray.ang.enums.NetworkType
import com.v2ray.ang.enums.Protocol
import com.v2ray.ang.util.JsonUtil

internal class OutboundConfigStep(
  private val convertProfile2Outbound: (ConnectionProfile) -> OutboundBean?,
) {

  fun applyOutbounds(v2rayConfig: V2rayConfig, connectionProfile: ConnectionProfile): V2rayConfig {
    val outbound = convertProfile2Outbound(connectionProfile) ?: throw OutboundConfigError(
      message = "Outbound mapping failed for protocol ${connectionProfile.protocol}",
      source = "OutboundConfigStep.applyOutbounds"
    )
    val ret = applyGlobalOutboundSettings(outbound)
    if (!ret) {
      throw OutboundConfigError(
        message = "Failed to apply global outbound settings",
        source = "OutboundConfigStep.applyGlobalOutboundSettings"
      )
    }

    if (v2rayConfig.outbounds.isNotEmpty()) {
      v2rayConfig.outbounds[0] = outbound
    } else {
      v2rayConfig.outbounds.add(outbound)
    }
    return applyOutboundFragment(v2rayConfig)
  }

  fun applyMoreOutbounds(v2rayConfig: V2rayConfig, subscriptionId: String): V2rayConfig {
    if (KeyValueStorage.decodeSettingsBool(AppConfig.PREF_FRAGMENT_ENABLED, false)) {
      return v2rayConfig
    }
    if (subscriptionId.isEmpty()) return v2rayConfig

    try {
      val subItem = KeyValueStorage.decodeSubscription(subscriptionId) ?: return v2rayConfig
      val outbound = v2rayConfig.outbounds[0]

      val prevNode = SettingsManager.getServerViaRemarks(subItem.prevProfile)
      if (prevNode != null) {
        val prevOutbound = convertProfile2Outbound(prevNode)
        if (prevOutbound != null) {
          applyGlobalOutboundSettings(prevOutbound)
          prevOutbound.tag = AppConfig.TAG_PROXY + "2"
          v2rayConfig.outbounds.add(prevOutbound)
          outbound.ensureSockopt().dialerProxy = prevOutbound.tag
        }
      }

      val nextNode = SettingsManager.getServerViaRemarks(subItem.nextProfile)
      if (nextNode != null) {
        val nextOutbound = convertProfile2Outbound(nextNode)
        if (nextOutbound != null) {
          applyGlobalOutboundSettings(nextOutbound)
          nextOutbound.tag = AppConfig.TAG_PROXY
          v2rayConfig.outbounds.add(0, nextOutbound)
          outbound.tag = AppConfig.TAG_PROXY + "1"
          nextOutbound.ensureSockopt().dialerProxy = outbound.tag
        }
      }
    } catch (runtime: RuntimeException) {
      Log.e(AppConfig.TAG, "Failed to configure more outbounds", runtime)
      throw OutboundConfigError(
        message = "Failed to configure more outbounds",
        source = "OutboundConfigStep.applyMoreOutbounds",
        cause = runtime
      )
    }
    return v2rayConfig
  }

  fun applyGlobalOutboundSettings(outbound: OutboundBean): Boolean {
    try {
      var muxEnabled = KeyValueStorage.decodeSettingsBool(AppConfig.PREF_MUX_ENABLED, false)
      val protocol = outbound.protocol
      if (protocol.equals(Protocol.ShadowSocks.name, true)
        || protocol.equals(Protocol.Socks.name, true)
        || protocol.equals(Protocol.Http.name, true)
        || protocol.equals(Protocol.Trojan.name, true)
        || protocol.equals(Protocol.WireGuard.name, true)
        || protocol.equals(Protocol.Hysteria2.name, true)
      ) {
        muxEnabled = false
      } else if (outbound.streamSettings?.network == NetworkType.XHTTP.type) {
        muxEnabled = false
      }

      if (muxEnabled) {
        outbound.mux?.enabled = true
        outbound.mux?.concurrency =
          KeyValueStorage.decodeSettingsString(AppConfig.PREF_MUX_CONCURRENCY, "8").orEmpty().toInt()
        outbound.mux?.xudpConcurrency =
          KeyValueStorage.decodeSettingsString(AppConfig.PREF_MUX_XUDP_CONCURRENCY, "16").orEmpty().toInt()
        outbound.mux?.xudpProxyUDP443 =
          KeyValueStorage.decodeSettingsString(AppConfig.PREF_MUX_XUDP_QUIC, "reject")
        if (protocol.equals(Protocol.Vless.name, true)
          && outbound.settings?.vnext?.first()?.users?.first()?.flow?.isNotEmpty() == true
        ) {
          outbound.mux?.concurrency = -1
        }
      } else {
        outbound.mux?.enabled = false
        outbound.mux?.concurrency = -1
      }

      if (protocol.equals(Protocol.WireGuard.name, true)) {
        var localTunAddr = if (outbound.settings?.address == null) {
          listOf(AppConfig.WIREGUARD_LOCAL_ADDRESS_V4)
        } else {
          outbound.settings?.address as List<*>
        }
        if (KeyValueStorage.decodeSettingsBool(AppConfig.PREF_PREFER_IPV6) != true) {
          localTunAddr = listOf(localTunAddr.first())
        }
        outbound.settings?.address = localTunAddr
      }

      if (outbound.streamSettings?.network == AppConfig.DEFAULT_NETWORK
        && outbound.streamSettings?.tcpSettings?.header?.type == AppConfig.HEADER_TYPE_HTTP
      ) {
        val path = outbound.streamSettings?.tcpSettings?.header?.request?.path
        val host = outbound.streamSettings?.tcpSettings?.header?.request?.headers?.Host

        val requestString: String by lazy {
          """{"version":"1.1","method":"GET","headers":{"User-Agent":["Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.6478.122 Mobile Safari/537.36"],"Accept-Encoding":["gzip, deflate"],"Connection":["keep-alive"],"Pragma":"no-cache"}}"""
        }
        outbound.streamSettings?.tcpSettings?.header?.request = JsonUtil.fromJson(
          requestString,
          StreamSettingsBean.TcpSettingsBean.HeaderBean.RequestBean::class.java
        )
        outbound.streamSettings?.tcpSettings?.header?.request?.path = if (path.isNullOrEmpty()) listOf("/") else path
        outbound.streamSettings?.tcpSettings?.header?.request?.headers?.Host = host
      }
    } catch (runtime: RuntimeException) {
      Log.e(AppConfig.TAG, "Failed to update outbound with global settings", runtime)
      return false
    }
    return true
  }

  fun applyOutboundFragment(v2rayConfig: V2rayConfig): V2rayConfig {
    try {
      if (!KeyValueStorage.decodeSettingsBool(AppConfig.PREF_FRAGMENT_ENABLED, false)) {
        return v2rayConfig
      }
      if (v2rayConfig.outbounds[0].streamSettings?.security != AppConfig.TLS
        && v2rayConfig.outbounds[0].streamSettings?.security != AppConfig.REALITY
      ) {
        return v2rayConfig
      }

      val fragmentOutbound = OutboundBean(
        protocol = AppConfig.PROTOCOL_FREEDOM,
        tag = AppConfig.TAG_FRAGMENT,
        mux = null
      )

      var packets = KeyValueStorage.decodeSettingsString(AppConfig.PREF_FRAGMENT_PACKETS) ?: "tlshello"
      if (v2rayConfig.outbounds[0].streamSettings?.security == AppConfig.REALITY && packets == "tlshello") {
        packets = "1-3"
      } else if (v2rayConfig.outbounds[0].streamSettings?.security == AppConfig.TLS && packets != "tlshello") {
        packets = "tlshello"
      }

      fragmentOutbound.settings = OutSettingsBean(
        fragment = OutSettingsBean.FragmentBean(
          packets = packets,
          length = KeyValueStorage.decodeSettingsString(AppConfig.PREF_FRAGMENT_LENGTH) ?: "50-100",
          interval = KeyValueStorage.decodeSettingsString(AppConfig.PREF_FRAGMENT_INTERVAL) ?: "10-20"
        ),
        noises = listOf(
          OutSettingsBean.NoiseBean(
            type = "rand",
            packet = "10-20",
            delay = "10-16",
          )
        ),
      )
      fragmentOutbound.streamSettings = StreamSettingsBean(
        sockopt = StreamSettingsBean.SockoptBean(
          TcpNoDelay = true,
          mark = 255
        )
      )
      v2rayConfig.outbounds.add(fragmentOutbound)

      v2rayConfig.outbounds[0].streamSettings?.sockopt = StreamSettingsBean.SockoptBean(
        dialerProxy = AppConfig.TAG_FRAGMENT
      )
    } catch (runtime: RuntimeException) {
      Log.e(AppConfig.TAG, "Failed to update outbound fragment", runtime)
      throw OutboundConfigError(
        message = "Failed to update outbound fragment",
        source = "OutboundConfigStep.applyOutboundFragment",
        cause = runtime
      )
    }
    return v2rayConfig
  }
}
