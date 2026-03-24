package com.v2ray.ang.handler

import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.V2rayConfig
import com.v2ray.ang.dto.ConnectionProfile

internal class ConfigAssembler(
  private val applyInbounds: (V2rayConfig) -> V2rayConfig?,
  private val applyOutbounds: (V2rayConfig, ConnectionProfile) -> V2rayConfig?,
  private val applyMoreOutbounds: (V2rayConfig, String) -> V2rayConfig,
  private val applyRouting: (V2rayConfig) -> V2rayConfig?,
  private val applyFakeDns: (V2rayConfig) -> V2rayConfig,
  private val applyDns: (V2rayConfig) -> V2rayConfig?,
  private val applyCustomLocalDns: (V2rayConfig) -> V2rayConfig?,
  private val applyResolveOutboundDomainsToHosts: (V2rayConfig) -> V2rayConfig,
) {

  fun applyStandardSteps(
    v2rayConfig: V2rayConfig,
    profile: ConnectionProfile,
  ): V2rayConfig? {
    return v2rayConfig
      .maybeThen { applyInbounds(it) }
      ?.maybeThen { applyOutbounds(it, profile) }
      ?.then { applyMoreOutbounds(it, profile.subscriptionId) }
      ?.maybeThen { applyRouting(it) }
      ?.then { applyFakeDns(it) }
      ?.maybeThen { applyDns(it) }
      ?.maybeThen {
        if (KeyValueStorage.decodeSettingsBool(AppConfig.PREF_LOCAL_DNS_ENABLED)) {
          applyCustomLocalDns(it)
        } else {
          it
        }
      }
      ?.then { it.applySpeedPolicyToggles() }
      ?.then { it.applyOptionalDomainResolve() }
  }

  private inline fun V2rayConfig.then(step: (V2rayConfig) -> V2rayConfig): V2rayConfig = step(this)

  private inline fun V2rayConfig.maybeThen(step: (V2rayConfig) -> V2rayConfig?): V2rayConfig? = step(this)

  private fun V2rayConfig.applySpeedPolicyToggles(): V2rayConfig {
    if (!KeyValueStorage.decodeSettingsBool(AppConfig.PREF_SPEED_ENABLED)) {
      stats = null
      policy = null
    }
    return this
  }

  private fun V2rayConfig.applyOptionalDomainResolve(): V2rayConfig {
    if (KeyValueStorage.decodeSettingsString(AppConfig.PREF_OUTBOUND_DOMAIN_RESOLVE_METHOD, "1") == "1") {
      applyResolveOutboundDomainsToHosts(this)
    }
    return this
  }
}
