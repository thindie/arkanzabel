package com.v2ray.ang.handler

import android.util.Log
import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.V2rayConfig
import com.v2ray.ang.dto.V2rayConfig.OutboundBean
import com.v2ray.ang.dto.V2rayConfig.RoutingBean.RulesBean
import com.v2ray.ang.error.DnsConfigError
import com.v2ray.ang.extension.isNotNullEmpty

internal class DnsConfigStep(
  private val getUserRule2Domain: (String) -> ArrayList<String>,
) {
  fun applyFakeDns(v2rayConfig: V2rayConfig): V2rayConfig {
    if (KeyValueStorage.decodeSettingsBool(AppConfig.PREF_LOCAL_DNS_ENABLED) && KeyValueStorage.decodeSettingsBool(AppConfig.PREF_FAKE_DNS_ENABLED)
    ) {
      v2rayConfig.fakedns = listOf(V2rayConfig.FakednsBean())
    }
    return v2rayConfig
  }

  fun applyCustomLocalDns(v2rayConfig: V2rayConfig): V2rayConfig {
    try {
      if (KeyValueStorage.decodeSettingsBool(AppConfig.PREF_FAKE_DNS_ENABLED)) {
        val geositeCn = arrayListOf(AppConfig.GEOSITE_CN)
        val proxyDomain = getUserRule2Domain(AppConfig.TAG_PROXY)
        val directDomain = getUserRule2Domain(AppConfig.TAG_DIRECT)
        v2rayConfig.dns?.servers?.add(
          0,
          V2rayConfig.DnsBean.ServersBean(
            address = "fakedns",
            domains = geositeCn.plus(proxyDomain).plus(directDomain)
          )
        )
      }

      if (SettingsManager.isVpnMode()) {
        if (SettingsManager.isUsingHevTun()) {
          v2rayConfig.routing.rules.add(
            0, RulesBean(
              inboundTag = arrayListOf("socks"),
              outboundTag = "dns-out",
              port = "53",
            )
          )
        } else {
          v2rayConfig.routing.rules.add(
            0, RulesBean(
              inboundTag = arrayListOf("tun"),
              outboundTag = "dns-out",
              port = "53",
            )
          )
        }
      }

      if (v2rayConfig.outbounds.none { e -> e.protocol == "dns" && e.tag == "dns-out" }) {
        v2rayConfig.outbounds.add(
          OutboundBean(
            protocol = "dns",
            tag = "dns-out",
            settings = null,
            streamSettings = null,
            mux = null
          )
        )
      }
    } catch (runtime: RuntimeException) {
      Log.e(AppConfig.TAG, "Failed to configure custom local DNS", runtime)
      throw DnsConfigError(
        message = "Failed to configure custom local DNS",
        source = "DnsConfigStep.applyCustomLocalDns",
        cause = runtime
      )
    }
    return v2rayConfig
  }

  fun applyDns(v2rayConfig: V2rayConfig): V2rayConfig {
    try {
      val hosts = mutableMapOf<String, Any>()
      val servers = ArrayList<Any>()

      val remoteDns = SettingsManager.getRemoteDnsServers()
      val proxyDomain = getUserRule2Domain(AppConfig.TAG_PROXY)
      remoteDns.forEach {
        servers.add(it)
      }
      if (proxyDomain.isNotEmpty()) {
        servers.add(
          V2rayConfig.DnsBean.ServersBean(
            address = remoteDns.first(),
            domains = proxyDomain,
          )
        )
      }

      val domesticDns = SettingsManager.getDomesticDnsServers()
      val directDomain = getUserRule2Domain(AppConfig.TAG_DIRECT)
      val isCnRoutingMode = directDomain.contains(AppConfig.GEOSITE_CN)
      val geoipCn = arrayListOf(AppConfig.GEOIP_CN)
      if (directDomain.isNotEmpty()) {
        servers.add(
          V2rayConfig.DnsBean.ServersBean(
            address = domesticDns.first(),
            domains = directDomain,
            expectIPs = if (isCnRoutingMode) geoipCn else null,
            skipFallback = true,
            tag = AppConfig.TAG_DOMESTIC_DNS
          )
        )
      }

      val blkDomain = getUserRule2Domain(AppConfig.TAG_BLOCKED)
      if (blkDomain.isNotEmpty()) {
        hosts.putAll(blkDomain.map { it to AppConfig.LOOPBACK })
      }

      hosts[AppConfig.GOOGLEAPIS_CN_DOMAIN] = AppConfig.GOOGLEAPIS_COM_DOMAIN
      hosts[AppConfig.DNS_ALIDNS_DOMAIN] = AppConfig.DNS_ALIDNS_ADDRESSES
      hosts[AppConfig.DNS_CLOUDFLARE_ONE_DOMAIN] = AppConfig.DNS_CLOUDFLARE_ONE_ADDRESSES
      hosts[AppConfig.DNS_CLOUDFLARE_DNS_COM_DOMAIN] = AppConfig.DNS_CLOUDFLARE_DNS_COM_ADDRESSES
      hosts[AppConfig.DNS_CLOUDFLARE_DNS_DOMAIN] = AppConfig.DNS_CLOUDFLARE_DNS_ADDRESSES
      hosts[AppConfig.DNS_DNSPOD_DOMAIN] = AppConfig.DNS_DNSPOD_ADDRESSES
      hosts[AppConfig.DNS_GOOGLE_DOMAIN] = AppConfig.DNS_GOOGLE_ADDRESSES
      hosts[AppConfig.DNS_QUAD9_DOMAIN] = AppConfig.DNS_QUAD9_ADDRESSES
      hosts[AppConfig.DNS_YANDEX_DOMAIN] = AppConfig.DNS_YANDEX_ADDRESSES

      try {
        val userHosts = KeyValueStorage.decodeSettingsString(AppConfig.PREF_DNS_HOSTS)
        if (userHosts.isNotNullEmpty()) {
          val userHostsMap = userHosts?.split(",")
            ?.filter { it.isNotEmpty() }
            ?.filter { it.contains(":") }
            ?.associate { it.split(":").let { (k, v) -> k to v } }
          if (userHostsMap != null) hosts.putAll(userHostsMap)
        }
      } catch (runtime: RuntimeException) {
        Log.e(AppConfig.TAG, "Failed to configure user DNS hosts", runtime)
        throw DnsConfigError(
          message = "Failed to parse user DNS hosts",
          source = "DnsConfigStep.applyDns.userHosts",
          cause = runtime
        )
      }

      v2rayConfig.dns = V2rayConfig.DnsBean(
        servers = servers,
        hosts = hosts,
        tag = AppConfig.TAG_DNS
      )

      v2rayConfig.routing.rules.add(
        RulesBean(
          outboundTag = AppConfig.TAG_DIRECT,
          inboundTag = arrayListOf(AppConfig.TAG_DOMESTIC_DNS),
          domain = null
        )
      )
      v2rayConfig.routing.rules.add(
        RulesBean(
          outboundTag = AppConfig.TAG_PROXY,
          inboundTag = arrayListOf(AppConfig.TAG_DNS),
          domain = null
        )
      )
    } catch (runtime: RuntimeException) {
      Log.e(AppConfig.TAG, "Failed to configure DNS", runtime)
      throw DnsConfigError(
        message = "Failed to configure DNS",
        source = "DnsConfigStep.applyDns",
        cause = runtime
      )
    }
    return v2rayConfig
  }
}
