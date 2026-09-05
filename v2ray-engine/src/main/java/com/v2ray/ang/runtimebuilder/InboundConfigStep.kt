package com.v2ray.ang.runtimebuilder

import com.thindie.engine.core.Log
import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.V2rayConfig
import com.v2ray.ang.enums.Protocol
import com.v2ray.ang.error.IncomingConfigError
import com.v2ray.ang.util.JsonUtil
import com.v2ray.ang.util.Utils

internal class InboundConfigStep(
  private val settings: SettingsReader,
  private val needTun: () -> Boolean,
) {
  fun applyInbounds(v2rayConfig: V2rayConfig): V2rayConfig {
    try {
      val socksPort = settings.getInt(AppConfig.PREF_SOCKS_PORT, AppConfig.PORT_SOCKS.toInt())
      val inbound1 = v2rayConfig.inbounds[0]

      if (!settings.getBool(AppConfig.PREF_PROXY_SHARING, false)) {
        inbound1.listen = AppConfig.LOOPBACK
      }
      inbound1.port = socksPort

      val fakedns = settings.getBool(AppConfig.PREF_FAKE_DNS_ENABLED, false)
      val sniffAllTlsAndHttp =
        settings.getBool(AppConfig.PREF_SNIFFING_ENABLED, true)
      inbound1.sniffing?.enabled = fakedns || sniffAllTlsAndHttp
      inbound1.sniffing?.routeOnly =
        settings.getBool(AppConfig.PREF_ROUTE_ONLY_ENABLED, false)
      if (!sniffAllTlsAndHttp) {
        inbound1.sniffing?.destOverride?.clear()
      }
      if (fakedns) {
        inbound1.sniffing?.destOverride?.add("fakedns")
      }

      if (!Utils.isXray()) {
        val inbound2 =
          JsonUtil.fromJson(JsonUtil.toJson(inbound1), V2rayConfig.Inbound::class.java)
            ?: throw IncomingConfigError(
              message = "Inbound mapping failed: cloned inbound is null",
              source = "InboundConfigStep.applyInbounds",
            )
        inbound2.tag = Protocol.Http.name.lowercase()
        inbound2.port = socksPort + if (Utils.isXray()) 0 else 1
        inbound2.protocol = Protocol.Http.name.lowercase()
        v2rayConfig.inbounds.add(inbound2)
      }

      if (needTun()) {
        val inboundTun = v2rayConfig.inbounds.firstOrNull { it.tag == "tun" }
        inboundTun?.settings?.mtu = settings.getInt(AppConfig.PREF_VPN_MTU, AppConfig.VPN_MTU)
        inboundTun?.sniffing = inbound1.sniffing
      }
    } catch (runtime: RuntimeException) {
      Log.e({ "Failed to configure inbounds" }, AppConfig.TAG, runtime)
      throw IncomingConfigError(
        message = "Failed to configure inbounds",
        source = "InboundConfigStep.applyInbounds",
        cause = runtime,
      )
    }
    return v2rayConfig
  }
}
