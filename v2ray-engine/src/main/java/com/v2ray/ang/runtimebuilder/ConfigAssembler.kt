package com.v2ray.ang.runtimebuilder

import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.ConnectionProfile
import com.v2ray.ang.dto.V2rayConfig
import com.v2ray.ang.error.ConfigBuildError
import com.v2ray.ang.runtime.KeyValueStorage

internal class ConfigAssembler(
  private val applyInbounds: (V2rayConfig) -> V2rayConfig,
  private val applyOutbounds: (V2rayConfig, ConnectionProfile) -> V2rayConfig,
  private val applyMoreOutbounds: (V2rayConfig, String) -> V2rayConfig,
  private val applyRouting: (V2rayConfig) -> V2rayConfig,
  private val applyFakeDns: (V2rayConfig) -> V2rayConfig,
  private val applyDns: (V2rayConfig) -> V2rayConfig,
  private val applyCustomLocalDns: (V2rayConfig) -> V2rayConfig,
  private val applyResolveOutboundDomainsToHosts: (V2rayConfig) -> V2rayConfig,
) {
  fun applyStandardSteps(
    v2rayConfig: V2rayConfig,
    connectionProfile: ConnectionProfile,
  ): V2rayConfig {
    return v2rayConfig
      .then { applyInbounds(it) }
      .then { applyOutbounds(it, connectionProfile) }
      .then { applyMoreOutbounds(it, connectionProfile.subscriptionId) }
      .then { applyRouting(it) }
      .then { applyFakeDns(it) }
      .then { applyDns(it) }
      .then {
        if (KeyValueStorage.decodeSettingsBool(AppConfig.PREF_LOCAL_DNS_ENABLED)) {
          applyCustomLocalDns(it)
        } else {
          it
        }
      }
      .then { it.applySpeedPolicyToggles() }
      .then { it.applyOptionalDomainResolve() }
  }

  private inline fun V2rayConfig.then(step: (V2rayConfig) -> V2rayConfig): V2rayConfig = step(this)

  private fun V2rayConfig.applySpeedPolicyToggles(): V2rayConfig {
    if (!KeyValueStorage.decodeSettingsBool(AppConfig.PREF_SPEED_ENABLED)) {
      stats = null
      policy = null
    }
    return this
  }

  private fun V2rayConfig.applyOptionalDomainResolve(): V2rayConfig {
    if (KeyValueStorage.decodeSettingsString(AppConfig.PREF_OUTBOUND_DOMAIN_RESOLVE_METHOD, "1") == "1") {
      return try {
        applyResolveOutboundDomainsToHosts(this)
      } catch (runtime: RuntimeException) {
        throw ConfigBuildError(
          message = "Failed to resolve outbound domains",
          stage = "domainResolve",
          cause = runtime,
        )
      }
    }
    return this
  }
}
