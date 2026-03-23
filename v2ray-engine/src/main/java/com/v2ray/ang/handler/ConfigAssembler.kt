package com.v2ray.ang.handler

import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.V2rayConfig
import com.v2ray.ang.dto.ConnectionProfile

internal class ConfigAssembler(
  private val applyInbounds: (V2rayConfig) -> Boolean,
  private val applyOutbounds: (V2rayConfig, ConnectionProfile) -> Boolean?,
  private val applyMoreOutbounds: (V2rayConfig, String) -> Boolean,
  private val applyRouting: (V2rayConfig) -> Boolean,
  private val applyFakeDns: (V2rayConfig) -> Unit,
  private val applyDns: (V2rayConfig) -> Boolean,
  private val applyCustomLocalDns: (V2rayConfig) -> Boolean,
  private val applyResolveOutboundDomainsToHosts: (V2rayConfig) -> Unit,
) {

  fun applyStandardSteps(v2rayConfig: V2rayConfig, profile: ConnectionProfile) {
    applyInbounds(v2rayConfig)
    applyOutbounds(v2rayConfig, profile) ?: return
    applyMoreOutbounds(v2rayConfig, profile.subscriptionId)
    applyRouting(v2rayConfig)
    applyFakeDns(v2rayConfig)
    applyDns(v2rayConfig)

    if (KeyValueStorage.decodeSettingsBool(AppConfig.PREF_LOCAL_DNS_ENABLED)) {
      applyCustomLocalDns(v2rayConfig)
    }

    if (!KeyValueStorage.decodeSettingsBool(AppConfig.PREF_SPEED_ENABLED)) {
      v2rayConfig.stats = null
      v2rayConfig.policy = null
    }

    if (KeyValueStorage.decodeSettingsString(AppConfig.PREF_OUTBOUND_DOMAIN_RESOLVE_METHOD, "1") == "1") {
      applyResolveOutboundDomainsToHosts(v2rayConfig)
    }
  }
}
