package com.v2ray.ang.runtimebuilder

import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.V2rayConfig
import com.v2ray.ang.dto.V2rayConfig.Outbound.StreamSettings
import com.v2ray.ang.runtime.KeyValueStorage
import com.v2ray.ang.util.HttpUtil

internal class DomainResolveStep {
  fun resolveOutboundDomainsToHosts(v2rayConfig: V2rayConfig): V2rayConfig {
    val proxyOutboundList = v2rayConfig.getAllProxyOutbound()
    val dns = v2rayConfig.dns ?: return v2rayConfig
    val newHosts = dns.hosts?.toMutableMap() ?: mutableMapOf()
    val preferIpv6 = KeyValueStorage.decodeSettingsBool(AppConfig.PREF_PREFER_IPV6)

    for (item in proxyOutboundList) {
      val domain = item.getServerAddress()
      if (domain.isNullOrEmpty()) continue

      if (newHosts.containsKey(domain)) {
        item.ensureSockopt().domainStrategy = "UseIP"
        item.ensureSockopt().happyEyeballs =
          StreamSettings.HappyEyeballs(
            prioritizeIPv6 = preferIpv6,
            interleave = 2,
          )
        continue
      }

      val resolvedIps = HttpUtil.resolveHostToIP(domain, preferIpv6)
      if (resolvedIps.isNullOrEmpty()) continue

      item.ensureSockopt().domainStrategy = "UseIP"
      item.ensureSockopt().happyEyeballs =
        StreamSettings.HappyEyeballs(
          prioritizeIPv6 = preferIpv6,
          interleave = 2,
        )
      newHosts[domain] =
        if (resolvedIps.size == 1) {
          resolvedIps[0]
        } else {
          resolvedIps
        }
    }

    dns.hosts = newHosts
    return v2rayConfig
  }
}
