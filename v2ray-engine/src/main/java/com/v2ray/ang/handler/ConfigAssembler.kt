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

  fun applyStandardSteps(
    v2rayConfig: V2rayConfig,
    profile: ConnectionProfile,
  ): V2rayConfig? {
    return v2rayConfig
      .thenIf { applyInbounds(it) }
      ?.maybeThen { if (applyOutbounds(it, profile) == true) it else null }
      ?.then { applyMoreOutbounds(it, profile.subscriptionId); it }
      ?.thenIf { applyRouting(it) }
      ?.then { applyFakeDns(it); it }
      ?.thenIf { applyDns(it) }
      ?.thenIf {
        if (KeyValueStorage.decodeSettingsBool(AppConfig.PREF_LOCAL_DNS_ENABLED)) {
          applyCustomLocalDns(it)
        } else {
          true
        }
      }
      ?.then {
        if (!KeyValueStorage.decodeSettingsBool(AppConfig.PREF_SPEED_ENABLED)) {
          it.stats = null
          it.policy = null
        }
        it
      }
      ?.then {
        if (KeyValueStorage.decodeSettingsString(AppConfig.PREF_OUTBOUND_DOMAIN_RESOLVE_METHOD, "1") == "1") {
          applyResolveOutboundDomainsToHosts(it)
        }
        it
      }
  }

  private inline fun V2rayConfig.thenIf(step: (V2rayConfig) -> Boolean): V2rayConfig? =
    if (step(this)) this else null

  private inline fun V2rayConfig.then(step: (V2rayConfig) -> V2rayConfig): V2rayConfig = step(this)

  private inline fun V2rayConfig.maybeThen(step: (V2rayConfig) -> V2rayConfig?): V2rayConfig? = step(this)
}
