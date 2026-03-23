package com.v2ray.ang.handler

import android.util.Log
import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.RulesetItem
import com.v2ray.ang.dto.V2rayConfig
import com.v2ray.ang.dto.V2rayConfig.RoutingBean.RulesBean
import com.v2ray.ang.util.JsonUtil

internal class RoutingConfigStep {

  fun applyRouting(v2rayConfig: V2rayConfig): Boolean {
    try {
      v2rayConfig.routing.domainStrategy =
        KeyValueStorage.decodeSettingsString(AppConfig.PREF_ROUTING_DOMAIN_STRATEGY)
          ?: "AsIs"

      val rulesetItems = KeyValueStorage.decodeRoutingRulesets()
      rulesetItems?.forEach { key ->
        applyRoutingUserRule(key, v2rayConfig)
      }
    } catch (e: Exception) {
      Log.e(AppConfig.TAG, "Failed to configure routing", e)
      return false
    }
    return true
  }

  fun getUserRule2Domain(tag: String): ArrayList<String> {
    val domain = ArrayList<String>()

    val rulesetItems = KeyValueStorage.decodeRoutingRulesets()
    rulesetItems?.forEach { key ->
      if (key.enabled && key.outboundTag == tag && !key.domain.isNullOrEmpty()) {
        key.domain?.forEach {
          if (it != AppConfig.GEOSITE_PRIVATE
            && (it.startsWith("geosite:") || it.startsWith("domain:"))
          ) {
            domain.add(it)
          }
        }
      }
    }

    return domain
  }

  private fun applyRoutingUserRule(item: RulesetItem?, v2rayConfig: V2rayConfig) {
    try {
      if (item == null || !item.enabled) {
        return
      }

      val rule = JsonUtil.fromJson(JsonUtil.toJson(item), RulesBean::class.java) ?: return
      v2rayConfig.routing.rules.add(rule)
    } catch (e: Exception) {
      Log.e(AppConfig.TAG, "Failed to apply routing user rule", e)
    }
  }
}
