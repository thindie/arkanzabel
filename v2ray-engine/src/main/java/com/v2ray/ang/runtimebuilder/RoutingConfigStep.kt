package com.v2ray.ang.runtimebuilder

import com.thindie.engine.core.Log
import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.RulesetItem
import com.v2ray.ang.dto.V2rayConfig
import com.v2ray.ang.dto.V2rayConfig.Routing.Rules
import com.v2ray.ang.error.RoutingConfigError
import com.v2ray.ang.util.JsonUtil

internal class RoutingConfigStep(
  private val settings: SettingsReader,
) {
  fun applyRouting(v2rayConfig: V2rayConfig): V2rayConfig {
    try {
      v2rayConfig.routing.domainStrategy =
        settings.getString(AppConfig.PREF_ROUTING_DOMAIN_STRATEGY)
          ?: "AsIs"

      val rulesetItems = settings.getRoutingRulesets()
      rulesetItems?.forEach { key ->
        applyRoutingUserRule(key, v2rayConfig)
      }
    } catch (runtime: RuntimeException) {
      Log.e({ "Failed to configure routing" }, AppConfig.TAG, runtime)
      throw RoutingConfigError(
        message = "Failed to configure routing",
        source = "RoutingConfigStep.applyRouting",
        cause = runtime,
      )
    }
    return v2rayConfig
  }

  fun getUserRule2Domain(tag: String): ArrayList<String> {
    val domain = ArrayList<String>()

    val rulesetItems = settings.getRoutingRulesets()
    rulesetItems?.forEach { key ->
      if (key.enabled && key.outboundTag == tag && !key.domain.isNullOrEmpty()) {
        key.domain?.forEach {
          if (it != AppConfig.GEOSITE_PRIVATE &&
            (it.startsWith("geosite:") || it.startsWith("domain:"))
          ) {
            domain.add(it)
          }
        }
      }
    }

    return domain
  }

  private fun applyRoutingUserRule(
    item: RulesetItem?,
    v2rayConfig: V2rayConfig,
  ) {
    try {
      if (item == null || !item.enabled) {
        return
      }

      val rule = JsonUtil.fromJson(JsonUtil.toJson(item), Rules::class.java) ?: return
      v2rayConfig.routing.rules.add(rule)
    } catch (runtime: RuntimeException) {
      Log.e({ "Failed to apply routing user rule" }, AppConfig.TAG, runtime)
      throw RoutingConfigError(
        message = "Failed to apply routing user rule",
        source = "RoutingConfigStep.applyRoutingUserRule",
        cause = runtime,
      )
    }
  }
}
