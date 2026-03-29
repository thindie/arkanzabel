package com.v2ray.ang.runtimebuilder

import android.util.Log
import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.V2rayConfig
import com.v2ray.ang.error.IncomingConfigError
import com.v2ray.ang.enums.Protocol
import com.v2ray.ang.runtime.KeyValueStorage
import com.v2ray.ang.runtime.SettingsManager
import com.v2ray.ang.util.JsonUtil
import com.v2ray.ang.util.Utils

internal class InboundConfigStep(
  private val needTun: () -> Boolean,
) {
  fun applyInbounds(v2rayConfig: V2rayConfig): V2rayConfig {
    try {
      val socksPort = SettingsManager.getSocksPort()
      val inbound1 = v2rayConfig.inbounds[0]

      if (!KeyValueStorage.decodeSettingsBool(AppConfig.PREF_PROXY_SHARING)) {
        inbound1.listen = AppConfig.LOOPBACK
      }
      inbound1.port = socksPort

      val fakedns = KeyValueStorage.decodeSettingsBool(AppConfig.PREF_FAKE_DNS_ENABLED)
      val sniffAllTlsAndHttp =
        KeyValueStorage.decodeSettingsBool(AppConfig.PREF_SNIFFING_ENABLED, true)
      inbound1.sniffing?.enabled = fakedns || sniffAllTlsAndHttp
      inbound1.sniffing?.routeOnly =
        KeyValueStorage.decodeSettingsBool(AppConfig.PREF_ROUTE_ONLY_ENABLED, false)
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
              source = "InboundConfigStep.applyInbounds"
            )
        inbound2.tag = Protocol.Http.name.lowercase()
        inbound2.port = SettingsManager.getHttpPort()
        inbound2.protocol = Protocol.Http.name.lowercase()
        v2rayConfig.inbounds.add(inbound2)
      }

      if (needTun()) {
        val inboundTun = v2rayConfig.inbounds.firstOrNull { it.tag == "tun" }
        inboundTun?.settings?.mtu = SettingsManager.getVpnMtu()
        inboundTun?.sniffing = inbound1.sniffing
      }
    } catch (runtime: RuntimeException) {
      Log.e(AppConfig.TAG, "Failed to configure inbounds", runtime)
      throw IncomingConfigError(
        message = "Failed to configure inbounds",
        source = "InboundConfigStep.applyInbounds",
        cause = runtime
      )
    }
    return v2rayConfig
  }
}
