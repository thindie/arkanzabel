package com.v2ray.ang.handler

import android.content.Context
import android.text.TextUtils
import android.util.Log
import com.google.gson.JsonArray
import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.ConfigResult
import com.v2ray.ang.dto.ConnectionProfile
import com.v2ray.ang.dto.V2rayConfig
import com.v2ray.ang.dto.V2rayConfig.OutboundBean
import com.v2ray.ang.dto.V2rayConfig.OutboundBean.OutSettingsBean
import com.v2ray.ang.dto.V2rayConfig.OutboundBean.StreamSettingsBean
import com.v2ray.ang.dto.V2rayConfig.RoutingBean.RulesBean
import com.v2ray.ang.enums.NetworkType
import com.v2ray.ang.enums.Protocol
import com.v2ray.ang.extension.isNotNullEmpty
import com.v2ray.ang.extension.nullIfBlank
import com.v2ray.ang.protocolstringsparsers.Http
import com.v2ray.ang.protocolstringsparsers.Hysteria2
import com.v2ray.ang.protocolstringsparsers.Shadowsocks
import com.v2ray.ang.protocolstringsparsers.Socks
import com.v2ray.ang.protocolstringsparsers.Trojan
import com.v2ray.ang.protocolstringsparsers.Vless
import com.v2ray.ang.protocolstringsparsers.Vmess
import com.v2ray.ang.protocolstringsparsers.Wireguard
import com.v2ray.ang.util.HttpUtil
import com.v2ray.ang.util.JsonUtil
import com.v2ray.ang.util.Utils

object V2rayConfigManager {
  private var initConfigCache: String? = null
  private var initConfigCacheWithTun: String? = null
  private val routingConfigStep by lazy { RoutingConfigStep() }
  private val dnsConfigStep by lazy { DnsConfigStep(::getUserRule2Domain) }
  private val outboundConfigStep by lazy { OutboundConfigStep(::convertProfile2Outbound) }
  private val configAssembler by lazy {
    ConfigAssembler(
      applyInbounds = ::getInbounds,
      applyOutbounds = ::getOutbounds,
      applyMoreOutbounds = ::getMoreOutbounds,
      applyRouting = ::getRouting,
      applyFakeDns = ::getFakeDns,
      applyDns = ::getDns,
      applyCustomLocalDns = ::getCustomLocalDns,
      applyResolveOutboundDomainsToHosts = ::resolveOutboundDomainsToHosts,
    )
  }

  //region get config function

  /**
   * Retrieves the V2ray configuration for the given GUID.
   *
   * @param context The context of the caller.
   * @param guid The unique identifier for the V2ray configuration.
   * @return A ConfigResult object containing the configuration details or indicating failure.
   */
  fun getV2rayConfig(context: Context, guid: String): ConfigResult {
    try {
      val config = KeyValueStorage.decodeServerConfig(guid) ?: return ConfigResult(false)
      return when (config.protocol) {
        Protocol.Custom -> getV2rayCustomConfig(context, guid)
        Protocol.PolicyGroup -> getV2rayGroupConfig(context, guid, config)
        else -> {
          getV2rayNormalConfig(context, guid, config)
        }
      }
    } catch (e: Exception) {
      Log.e(AppConfig.TAG, "Failed to get V2ray config", e)
      return ConfigResult(false)
    }
  }

  /**
   * Retrieves the speedtest V2ray configuration for the given GUID.
   *
   * @param context The context of the caller.
   * @param guid The unique identifier for the V2ray configuration.
   * @return A ConfigResult object containing the configuration details or indicating failure.
   */
  fun getV2rayConfig4Speedtest(context: Context, guid: String): ConfigResult {
    try {
      val config = KeyValueStorage.decodeServerConfig(guid) ?: return ConfigResult(false)
      return when (config.protocol) {
        Protocol.Custom -> getV2rayCustomConfig(context, guid)
        Protocol.PolicyGroup -> { // The number of policy groups will not be very large, so no special handling is needed.
          getV2rayGroupConfig(context, guid, config)
        }

        else -> {
          getV2rayNormalConfig4Speedtest(context, guid, config)
        }
      }
    } catch (e: Exception) {
      Log.e(AppConfig.TAG, "Failed to get V2ray config for speedtest", e)
      return ConfigResult(false)
    }
  }

  /**
   * Retrieves the custom V2ray configuration.
   *
   * @param guid The unique identifier for the V2ray configuration.
   * @param config The profile item containing the configuration details.
   * @return A ConfigResult object containing the result of the configuration retrieval.
   */
  private fun getV2rayCustomConfig(
    context: Context,
    guid: String,
  ): ConfigResult {
    val raw = KeyValueStorage.decodeServerRaw(guid) ?: return ConfigResult(false)
    val result = ConfigResult(true, guid, raw)
    if (!needTun()) {
      return result
    }

    // check if tun inbound exists
    val json = JsonUtil.parseString(raw) ?: return result
    val inboundsJson = if (json.has("inbounds") && json.get("inbounds")?.isJsonNull == false) {
      json.getAsJsonArray("inbounds")
    } else {
      JsonArray()
    }

    for (i in 0 until inboundsJson.size()) {
      val elem = inboundsJson.get(i)
      if (elem.isJsonObject) {
        val inb = elem.asJsonObject
        val tag =
          if (inb.has("tag") && inb.get("tag")?.isJsonNull == false) inb.get("tag").asString else ""
        if (tag == "tun") return result
      }
    }

    // add tun inbound from template
    val templateConfig = initV2rayConfig(context) ?: return result
    val inboundTun = templateConfig.inbounds.firstOrNull { it.tag == "tun" } ?: return result
    inboundTun.settings?.mtu = SettingsManager.getVpnMtu()

    // add to json
    inboundsJson.add(JsonUtil.parseString(JsonUtil.toJson(inboundTun)))
    if (inboundsJson.size() == 1) {
      json.add("inbounds", inboundsJson)
    }

    val updatedRaw = JsonUtil.toJsonPretty(json) ?: return result
    return ConfigResult(true, guid, updatedRaw)
  }

  /**
   * Retrieves the group V2ray configuration.
   *
   * @param context The context in which the function is called.
   * @param guid The unique identifier for the V2ray configuration.
   * @param config The profile item containing the configuration details.
   * @return A ConfigResult object containing the result of the configuration retrieval.
   */
  private fun getV2rayGroupConfig(
    context: Context,
    guid: String,
    config: ConnectionProfile,
  ): ConfigResult {
    val result = ConfigResult(false)

    val serverList = KeyValueStorage.decodeServerList()
    val configList = serverList
      .mapNotNull { id -> KeyValueStorage.decodeServerConfig(id) }
      .filter { profile ->
        val subscriptionId = config.policyGroupSubscriptionId
        if (subscriptionId.isNullOrBlank()) {
          true
        } else {
          profile.subscriptionId == subscriptionId
        }
      }
      .filter { profile ->
        val filter = config.policyGroupFilter
        if (filter.isNullOrBlank()) {
          true
        } else {
          try {
            Regex(filter).containsMatchIn(profile.remarks)
          } catch (e: Exception) {
            profile.remarks.contains(filter)
          }
        }
      }

    val v2rayConfig = getV2rayMultipleConfig(context, config, configList) ?: return result

    return result.copy(
      status = true,
      content = JsonUtil.toJsonPretty(v2rayConfig) ?: "",
      guid = guid,
    )
  }

  /**
   * Retrieves the normal V2ray configuration.
   *
   * @param context The context in which the function is called.
   * @param guid The unique identifier for the V2ray configuration.
   * @param config The profile item containing the configuration details.
   * @return A ConfigResult object containing the result of the configuration retrieval.
   */
  private fun getV2rayNormalConfig(
    context: Context,
    guid: String,
    config: ConnectionProfile,
  ): ConfigResult {
    val result = ConfigResult(false)

    val address = config.server ?: return result
    if (!Utils.isPureIpAddress(address)) {
      if (!Utils.isValidUrl(address)) {
        Log.w(AppConfig.TAG, "$address is an invalid ip or domain")
        return result
      }
    }

    val v2rayConfig = initV2rayConfig(context) ?: return result
    v2rayConfig.log.loglevel =
      KeyValueStorage.decodeSettingsString(AppConfig.PREF_LOGLEVEL) ?: "warning"
    v2rayConfig.remarks = config.remarks

    val assembled = configAssembler.applyStandardSteps(v2rayConfig, config) ?: return result
    if (assembled.getProxyOutbound() == null) return result
    return result.copy(
      status = true,
      content = JsonUtil.toJsonPretty(assembled) ?: "",
      guid = guid,
    )
  }

  private fun getV2rayMultipleConfig(
    context: Context,
    config: ConnectionProfile,
    configList: List<ConnectionProfile>,
  ): V2rayConfig? {
    val validConfigs = configList.asSequence().filter { it.server.isNotNullEmpty() }
      .filter { !Utils.isPureIpAddress(it.server!!) || Utils.isValidUrl(it.server!!) }
      .filter { it.protocol != Protocol.Custom }
      .filter { it.protocol != Protocol.PolicyGroup }
      .toList()

    if (validConfigs.isEmpty()) {
      Log.w(AppConfig.TAG, "All configs are invalid")
      return null
    }

    val v2rayConfig = initV2rayConfig(context) ?: return null
    v2rayConfig.log.loglevel =
      KeyValueStorage.decodeSettingsString(AppConfig.PREF_LOGLEVEL) ?: "warning"
    v2rayConfig.remarks = config.remarks

    getInbounds(v2rayConfig)

    v2rayConfig.outbounds.removeAt(0)
    val outboundsList = mutableListOf<OutboundBean>()
    var index = 0
    for (config in validConfigs) {
      index++
      val outbound = convertProfile2Outbound(config) ?: continue
      val ret = updateOutboundWithGlobalSettings(outbound)
      if (!ret) continue
      outbound.tag = "proxy-$index"
      outboundsList.add(outbound)
    }
    outboundsList.addAll(v2rayConfig.outbounds)
    v2rayConfig.outbounds = ArrayList(outboundsList)

    getRouting(v2rayConfig)

    getFakeDns(v2rayConfig)

    getDns(v2rayConfig)

    getBalance(v2rayConfig, config)

    if (KeyValueStorage.decodeSettingsBool(AppConfig.PREF_LOCAL_DNS_ENABLED)) {
      getCustomLocalDns(v2rayConfig)
    }
    if (!KeyValueStorage.decodeSettingsBool(AppConfig.PREF_SPEED_ENABLED)) {
      v2rayConfig.stats = null
      v2rayConfig.policy = null
    }

    //Resolve and add to DNS Hosts
    if (KeyValueStorage.decodeSettingsString(
        AppConfig.PREF_OUTBOUND_DOMAIN_RESOLVE_METHOD,
        "1"
      ) == "1"
    ) {
      resolveOutboundDomainsToHosts(v2rayConfig)
    }

    return v2rayConfig
  }

  /**
   * Retrieves the normal V2ray configuration for speedtest.
   *
   * @param context The context in which the function is called.
   * @param guid The unique identifier for the V2ray configuration.
   * @param config The profile item containing the configuration details.
   * @return A ConfigResult object containing the result of the configuration retrieval.
   */
  private fun getV2rayNormalConfig4Speedtest(
    context: Context,
    guid: String,
    config: ConnectionProfile,
  ): ConfigResult {
    val result = ConfigResult(false)

    val address = config.server ?: return result
    if (!Utils.isPureIpAddress(address)) {
      if (!Utils.isValidUrl(address)) {
        Log.w(AppConfig.TAG, "$address is an invalid ip or domain")
        return result
      }
    }

    val v2rayConfig = initV2rayConfig(context) ?: return result

    getOutbounds(v2rayConfig, config) ?: return result
    getMoreOutbounds(v2rayConfig, config.subscriptionId)

    v2rayConfig.log.loglevel =
      KeyValueStorage.decodeSettingsString(AppConfig.PREF_LOGLEVEL) ?: "warning"
    v2rayConfig.inbounds.clear()
    v2rayConfig.routing.rules.clear()
    v2rayConfig.dns = null
    v2rayConfig.fakedns = null
    v2rayConfig.stats = null
    v2rayConfig.policy = null

    v2rayConfig.outbounds.forEach { key ->
      key.mux = null
    }
    return result.copy(
      status = true,
      content = JsonUtil.toJsonPretty(v2rayConfig) ?: "",
      guid = guid
    )
  }

  /**
   * Initializes V2ray configuration.
   *
   * This function loads the V2ray configuration from assets or from a cached value.
   * It first attempts to use the cached configuration if available, otherwise reads
   * the configuration from the "v2ray_config.json" asset file.
   *
   * @param context Android context used to access application assets
   * @return V2rayConfig object parsed from the JSON configuration, or null if the configuration is empty
   */
  private fun initV2rayConfig(context: Context): V2rayConfig? {
    var assets = ""
    if (needTun()) {
      assets =
        initConfigCacheWithTun ?: Utils.readTextFromAssets(context, "v2ray_config_with_tun.json")
      if (TextUtils.isEmpty(assets)) {
        return null
      }
      initConfigCacheWithTun = assets
    } else {
      assets = initConfigCache ?: Utils.readTextFromAssets(context, "v2ray_config.json")
      if (TextUtils.isEmpty(assets)) {
        return null
      }
      initConfigCache = assets
    }
    val config = JsonUtil.fromJson(assets, V2rayConfig::class.java)
    return config
  }


  //endregion


  //region some sub function

  private fun needTun(): Boolean {
    return SettingsManager.isVpnMode() && !SettingsManager.isUsingHevTun()
  }

  /**
   * Configures the inbound settings for V2ray.
   *
   * This function sets up the listening ports, sniffing options, and other inbound-related configurations.
   *
   * @param v2rayConfig The V2ray configuration object to be modified
   * @return true if inbound configuration was successful, false otherwise
   */
  private fun getInbounds(v2rayConfig: V2rayConfig): Boolean {
    try {
      val socksPort = SettingsManager.getSocksPort()
      val inbound1 = v2rayConfig.inbounds[0]

      if (KeyValueStorage.decodeSettingsBool(AppConfig.PREF_PROXY_SHARING) != true) {
        inbound1.listen = AppConfig.LOOPBACK
      }
      inbound1.port = socksPort
      val fakedns = KeyValueStorage.decodeSettingsBool(AppConfig.PREF_FAKE_DNS_ENABLED) == true
      val sniffAllTlsAndHttp =
        KeyValueStorage.decodeSettingsBool(AppConfig.PREF_SNIFFING_ENABLED, true) != false
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
          JsonUtil.fromJson(JsonUtil.toJson(inbound1), V2rayConfig.InboundBean::class.java)
            ?: return false
        inbound2.tag = Protocol.Http.name.lowercase()
        inbound2.port = SettingsManager.getHttpPort()
        inbound2.protocol = Protocol.Http.name.lowercase()
        v2rayConfig.inbounds.add(inbound2)
      }

      if (needTun()) {
        val inboundTun = v2rayConfig.inbounds.firstOrNull { e -> e.tag == "tun" }
        inboundTun?.settings?.mtu = SettingsManager.getVpnMtu()
        inboundTun?.sniffing = inbound1.sniffing
      }
    } catch (e: Exception) {
      Log.e(AppConfig.TAG, "Failed to configure inbounds", e)
      return false
    }
    return true
  }

  /**
   * Configures the fake DNS settings if enabled.
   *
   * Adds FakeDNS configuration to v2rayConfig if both local DNS and fake DNS are enabled.
   *
   * @param v2rayConfig The V2ray configuration object to be modified
   */
  private fun getFakeDns(v2rayConfig: V2rayConfig) {
    dnsConfigStep.applyFakeDns(v2rayConfig)
  }

  /**
   * Configures routing settings for V2ray.
   *
   * Sets up the domain strategy and adds routing rules from saved rulesets.
   *
   * @param v2rayConfig The V2ray configuration object to be modified
   * @return true if routing configuration was successful, false otherwise
   */
  private fun getRouting(v2rayConfig: V2rayConfig): Boolean {
    return routingConfigStep.applyRouting(v2rayConfig)
  }

  /**
   * Retrieves domain rules for a specific outbound tag.
   *
   * Searches through all rulesets to find domains targeting the specified tag.
   *
   * @param tag The outbound tag to search for
   * @return ArrayList of domain rules matching the tag
   */
  private fun getUserRule2Domain(tag: String): ArrayList<String> {
    return routingConfigStep.getUserRule2Domain(tag)
  }

  /**
   * Configures custom local DNS settings.
   *
   * Sets up DNS inbound, outbound, and routing rules for local DNS resolution.
   *
   * @param v2rayConfig The V2ray configuration object to be modified
   * @return true if custom local DNS configuration was successful, false otherwise
   */
  private fun getCustomLocalDns(v2rayConfig: V2rayConfig): Boolean {
    return dnsConfigStep.applyCustomLocalDns(v2rayConfig)
  }

  /**
   * Configures the DNS settings for V2ray.
   *
   * Sets up DNS servers, hosts, and routing rules for DNS resolution.
   *
   * @param v2rayConfig The V2ray configuration object to be modified
   * @return true if DNS configuration was successful, false otherwise
   */
  private fun getDns(v2rayConfig: V2rayConfig): Boolean {
    return dnsConfigStep.applyDns(v2rayConfig)
  }


  //endregion


  //region outbound related functions

  /**
   * Configures the primary outbound connection.
   *
   * Converts the profile to an outbound configuration and applies global settings.
   *
   * @param v2rayConfig The V2ray configuration object to be modified
   * @param config The profile item containing connection details
   * @return true if outbound configuration was successful, null if there was an error
   */
  private fun getOutbounds(v2rayConfig: V2rayConfig, config: ConnectionProfile): Boolean? {
    return outboundConfigStep.applyOutbounds(v2rayConfig, config)
  }

  /**
   * Configures additional outbound connections for proxy chaining.
   *
   * Sets up previous and next proxies in a subscription for advanced routing capabilities.
   *
   * @param v2rayConfig The V2ray configuration object to be modified
   * @param subscriptionId The subscription ID to look up related proxies
   * @return true if additional outbounds were configured successfully, false otherwise
   */
  private fun getMoreOutbounds(v2rayConfig: V2rayConfig, subscriptionId: String): Boolean {
    return outboundConfigStep.applyMoreOutbounds(v2rayConfig, subscriptionId)
  }

  /**
   * Updates outbound settings based on global preferences.
   *
   * Applies multiplexing and protocol-specific settings to an outbound connection.
   *
   * @param outbound The outbound connection to update
   * @return true if the update was successful, false otherwise
   */
  private fun updateOutboundWithGlobalSettings(outbound: OutboundBean): Boolean {
    return outboundConfigStep.applyGlobalOutboundSettings(outbound)
  }

  /**
   * Configures load balancing settings for the V2ray configuration.
   *
   * @param v2rayConfig The V2ray configuration object to be modified with balancing settings
   * @param config The profile item containing policy group settings
   */
  private fun getBalance(v2rayConfig: V2rayConfig, config: ConnectionProfile) {
    try {
      v2rayConfig.routing.rules.forEach { rule ->
        if (rule.outboundTag == "proxy") {
          rule.outboundTag = null
          rule.balancerTag = AppConfig.TAG_BALANCER
        }
      }

      val lstSelector = listOf("proxy-")
      when (config.policyGroupType) {
        // Least Ping goto else
        "1" -> {
          // Least Load
          val balancer = V2rayConfig.RoutingBean.BalancerBean(
            tag = AppConfig.TAG_BALANCER,
            selector = lstSelector,
            strategy = V2rayConfig.RoutingBean.StrategyObject(
              type = "leastLoad"
            )
          )
          v2rayConfig.routing.balancers = listOf(balancer)
          v2rayConfig.burstObservatory = V2rayConfig.BurstObservatoryObject(
            subjectSelector = lstSelector,
            pingConfig = V2rayConfig.BurstObservatoryObject.PingConfigObject(
              destination = KeyValueStorage.decodeSettingsString(AppConfig.PREF_DELAY_TEST_URL)
                ?: AppConfig.DELAY_TEST_URL,
              interval = "5m",
              sampling = 2,
              timeout = "30s"
            )
          )
        }

        "2" -> {
          // Random
          val balancer = V2rayConfig.RoutingBean.BalancerBean(
            tag = AppConfig.TAG_BALANCER,
            selector = lstSelector,
            strategy = V2rayConfig.RoutingBean.StrategyObject(
              type = "random"
            )
          )
          v2rayConfig.routing.balancers = listOf(balancer)
        }

        "3" -> {
          // Round Robin
          val balancer = V2rayConfig.RoutingBean.BalancerBean(
            tag = AppConfig.TAG_BALANCER,
            selector = lstSelector,
            strategy = V2rayConfig.RoutingBean.StrategyObject(
              type = "roundRobin"
            )
          )
          v2rayConfig.routing.balancers = listOf(balancer)
        }

        else -> {
          // Default: Least Ping
          val balancer = V2rayConfig.RoutingBean.BalancerBean(
            tag = AppConfig.TAG_BALANCER,
            selector = lstSelector,
            strategy = V2rayConfig.RoutingBean.StrategyObject(
              type = "leastPing"
            )
          )
          v2rayConfig.routing.balancers = listOf(balancer)
          v2rayConfig.observatory = V2rayConfig.ObservatoryObject(
            subjectSelector = lstSelector,
            probeUrl = KeyValueStorage.decodeSettingsString(AppConfig.PREF_DELAY_TEST_URL)
              ?: AppConfig.DELAY_TEST_URL,
            probeInterval = "3m",
            enableConcurrency = true
          )
        }
      }

      if (v2rayConfig.routing.domainStrategy == "IPIfNonMatch") {
        v2rayConfig.routing.rules.add(
          RulesBean(
            ip = arrayListOf("0.0.0.0/0", "::/0"),
            balancerTag = AppConfig.TAG_BALANCER,
          )
        )
      } else {
        v2rayConfig.routing.rules.add(
          RulesBean(
            network = "tcp,udp",
            balancerTag = AppConfig.TAG_BALANCER,
          )
        )
      }
    } catch (e: Exception) {
      Log.e(AppConfig.TAG, "Failed to configure balance", e)
    }
  }

  /**
   * Updates the outbound with fragment settings for traffic optimization.
   *
   * Configures packet fragmentation for TLS and REALITY protocols if enabled.
   *
   * @param v2rayConfig The V2ray configuration object to be modified
   * @return true if fragment configuration was successful, false otherwise
   */
  private fun updateOutboundFragment(v2rayConfig: V2rayConfig): Boolean {
    return outboundConfigStep.applyOutboundFragment(v2rayConfig)
  }

  /**
   * Resolves domain names to IP addresses in outbound connections.
   *
   * Pre-resolves domains to improve connection speed and reliability.
   *
   * @param v2rayConfig The V2ray configuration object to be modified
   */
  private fun resolveOutboundDomainsToHosts(v2rayConfig: V2rayConfig) {
    val proxyOutboundList = v2rayConfig.getAllProxyOutbound()
    val dns = v2rayConfig.dns ?: return
    val newHosts = dns.hosts?.toMutableMap() ?: mutableMapOf()
    val preferIpv6 = KeyValueStorage.decodeSettingsBool(AppConfig.PREF_PREFER_IPV6) == true

    for (item in proxyOutboundList) {
      val domain = item.getServerAddress()
      if (domain.isNullOrEmpty()) continue

      if (newHosts.containsKey(domain)) {
        item.ensureSockopt().domainStrategy = "UseIP"
        item.ensureSockopt().happyEyeballs = StreamSettingsBean.HappyEyeballsBean(
          prioritizeIPv6 = preferIpv6,
          interleave = 2
        )
        continue
      }

      val resolvedIps = HttpUtil.resolveHostToIP(domain, preferIpv6)
      if (resolvedIps.isNullOrEmpty()) continue

      item.ensureSockopt().domainStrategy = "UseIP"
      item.ensureSockopt().happyEyeballs = StreamSettingsBean.HappyEyeballsBean(
        prioritizeIPv6 = preferIpv6,
        interleave = 2
      )
      newHosts[domain] = if (resolvedIps.size == 1) {
        resolvedIps[0]
      } else {
        resolvedIps
      }
    }

    dns.hosts = newHosts
  }

  /**
   * Converts a profile item to an outbound configuration.
   *
   * Creates appropriate outbound settings based on the protocol type.
   *
   * @param connectionProfile The profile item to convert
   * @return OutboundBean configuration for the profile, or null if not supported
   */
  private fun convertProfile2Outbound(connectionProfile: ConnectionProfile): OutboundBean? {
    return when (connectionProfile.protocol) {
      Protocol.Vmess -> Vmess.toOutbound(connectionProfile)
      Protocol.Custom -> null
      Protocol.ShadowSocks -> Shadowsocks.toOutbound(connectionProfile)
      Protocol.Socks -> Socks.toOutbound(connectionProfile)
      Protocol.Vless -> Vless.toOutbound(connectionProfile)
      Protocol.Trojan -> Trojan.toOutbound(connectionProfile)
      Protocol.WireGuard -> Wireguard.toOutbound(connectionProfile)
      Protocol.Hysteria2 -> Hysteria2.toOutbound(connectionProfile)
      Protocol.Http -> Http.toOutbound(connectionProfile)
      Protocol.PolicyGroup -> null
      else -> null
    }
  }

  /**
   * Creates an initial outbound configuration for a specific protocol type.
   *
   * Provides a template configuration for different protocol types.
   *
   * @param configType The type of configuration to create
   * @return An initial OutboundBean for the specified configuration type, or null for custom types
   */
  fun createInitOutbound(configType: Protocol): OutboundBean? {
    return when (configType) {
      Protocol.Vmess,
      Protocol.Vless,
        ->
        return OutboundBean(
          protocol = configType.name.lowercase(),
          settings = OutSettingsBean(
            vnext = listOf(
              OutSettingsBean.VnextBean(
                users = listOf(OutSettingsBean.VnextBean.UsersBean())
              )
            )
          ),
          streamSettings = StreamSettingsBean()
        )

      Protocol.ShadowSocks,
      Protocol.Socks,
      Protocol.Http,
      Protocol.Trojan,
        ->
        return OutboundBean(
          protocol = configType.name.lowercase(),
          settings = OutSettingsBean(
            servers = listOf(OutSettingsBean.ServersBean())
          ),
          streamSettings = StreamSettingsBean()
        )

      Protocol.WireGuard ->
        return OutboundBean(
          protocol = configType.name.lowercase(),
          settings = OutSettingsBean(
            secretKey = "",
            peers = listOf(OutSettingsBean.WireGuardBean())
          )
        )

      Protocol.Hysteria,
      Protocol.Hysteria2,
        ->
        return OutboundBean(
          protocol = Protocol.Hysteria.name.lowercase(),
          settings = OutSettingsBean(
            servers = null
          ),
          streamSettings = StreamSettingsBean()
        )

      Protocol.Custom -> null
      Protocol.PolicyGroup -> null
    }
  }

  /**
   * Configures transport settings for an outbound connection.
   *
   * Sets up protocol-specific transport options based on the profile settings.
   *
   * @param streamSettings The stream settings to configure
   * @param connectionProfile The profile containing transport configuration
   * @return The Server Name Indication (SNI) value to use, or null if not applicable
   */
  fun populateTransportSettings(
    streamSettings: StreamSettingsBean,
    connectionProfile: ConnectionProfile,
  ): String? {
    val transport = connectionProfile.network.orEmpty()
    val headerType = connectionProfile.headerType
    val host = connectionProfile.host
    val path = connectionProfile.path
    val seed = connectionProfile.seed
//        val quicSecurity = profileItem.quicSecurity
//        val key = profileItem.quicKey
    val mode = connectionProfile.mode
    val serviceName = connectionProfile.serviceName
    val authority = connectionProfile.authority
    val xhttpMode = connectionProfile.xhttpMode
    val xhttpExtra = connectionProfile.xhttpExtra

    var sni: String? = null
    streamSettings.network = transport.ifEmpty { NetworkType.TCP.type }
    when (streamSettings.network) {
      NetworkType.TCP.type -> {
        val tcpSetting = StreamSettingsBean.TcpSettingsBean()
        if (headerType == AppConfig.HEADER_TYPE_HTTP) {
          tcpSetting.header.type = AppConfig.HEADER_TYPE_HTTP
          if (!TextUtils.isEmpty(host) || !TextUtils.isEmpty(path)) {
            val requestObj = StreamSettingsBean.TcpSettingsBean.HeaderBean.RequestBean()
            requestObj.headers.Host =
              host.orEmpty().split(",").map { it.trim() }.filter { it.isNotEmpty() }
            requestObj.path = path.orEmpty().split(",").map { it.trim() }.filter { it.isNotEmpty() }
            tcpSetting.header.request = requestObj
            sni = requestObj.headers.Host?.getOrNull(0)
          }
        } else {
          tcpSetting.header.type = "none"
          sni = host
        }
        streamSettings.tcpSettings = tcpSetting
      }

      NetworkType.KCP.type -> {
        streamSettings.kcpSettings = StreamSettingsBean.KcpSettingsBean()
        val udpMaskList = mutableListOf<StreamSettingsBean.FinalMaskBean.MaskBean>()
        if (!headerType.isNullOrEmpty() && headerType != "none") {
          val kcpHeaderType = when {
            headerType == "wechat-video" -> "header-wechat"
            else -> "header-$headerType"
          }
          udpMaskList.add(
            StreamSettingsBean.FinalMaskBean.MaskBean(
              type = kcpHeaderType,
              settings = if (headerType == "dns" && !host.isNullOrEmpty()) {
                StreamSettingsBean.FinalMaskBean.MaskBean.MaskSettingsBean(
                  domain = host
                )
              } else {
                null
              }
            )
          )
        }
        if (seed.isNullOrEmpty()) {
          udpMaskList.add(
            StreamSettingsBean.FinalMaskBean.MaskBean(
              type = "mkcp-original"
            )
          )
        } else {
          udpMaskList.add(
            StreamSettingsBean.FinalMaskBean.MaskBean(
              type = "mkcp-aes128gcm",
              settings = StreamSettingsBean.FinalMaskBean.MaskBean.MaskSettingsBean(
                password = seed
              )
            )
          )
        }
        streamSettings.finalmask = StreamSettingsBean.FinalMaskBean(
          udp = udpMaskList.toList()
        )
      }

      NetworkType.WS.type -> {
        val wssetting = StreamSettingsBean.WsSettingsBean()
        wssetting.headers.Host = host.orEmpty()
        sni = host
        wssetting.path = path ?: "/"
        streamSettings.wsSettings = wssetting
      }

      NetworkType.HTTP_UPGRADE.type -> {
        val httpupgradeSetting = StreamSettingsBean.HttpupgradeSettingsBean()
        httpupgradeSetting.host = host.orEmpty()
        sni = host
        httpupgradeSetting.path = path ?: "/"
        streamSettings.httpupgradeSettings = httpupgradeSetting
      }

      NetworkType.XHTTP.type -> {
        val xhttpSetting = StreamSettingsBean.XhttpSettingsBean()
        xhttpSetting.host = host.orEmpty()
        sni = host
        xhttpSetting.path = path ?: "/"
        xhttpSetting.mode = xhttpMode
        xhttpSetting.extra = JsonUtil.parseString(xhttpExtra.orEmpty())
        streamSettings.xhttpSettings = xhttpSetting
      }

      NetworkType.H2.type, NetworkType.HTTP.type -> {
        streamSettings.network = NetworkType.H2.type
        val h2Setting = StreamSettingsBean.HttpSettingsBean()
        h2Setting.host = host.orEmpty().split(",").map { it.trim() }.filter { it.isNotEmpty() }
        sni = h2Setting.host.getOrNull(0)
        h2Setting.path = path ?: "/"
        streamSettings.httpSettings = h2Setting
      }

//                    "quic" -> {
//                        val quicsetting = QuicSettingBean()
//                        quicsetting.security = quicSecurity ?: "none"
//                        quicsetting.key = key.orEmpty()
//                        quicsetting.header.type = headerType ?: "none"
//                        quicSettings = quicsetting
//                    }

      NetworkType.GRPC.type -> {
        val grpcSetting = StreamSettingsBean.GrpcSettingsBean()
        grpcSetting.multiMode = mode == "multi"
        grpcSetting.serviceName = serviceName.orEmpty()
        grpcSetting.authority = authority.orEmpty()
        grpcSetting.idle_timeout = 60
        grpcSetting.health_check_timeout = 20
        sni = authority
        streamSettings.grpcSettings = grpcSetting
      }

      NetworkType.HYSTERIA.type -> {
        val hysteriaSetting = StreamSettingsBean.HysteriaSettingsBean(
          version = 2,
          auth = connectionProfile.password.orEmpty(),
          up = connectionProfile.bandwidthUp?.ifEmpty { "0" }.orEmpty(),
          down = connectionProfile.bandwidthDown?.ifEmpty { "0" }.orEmpty(),
          udphop = null
        )
        if (connectionProfile.portHopping.isNotNullEmpty()) {
          hysteriaSetting.udphop = StreamSettingsBean.HysteriaSettingsBean.HysteriaUdpHopBean(
            port = connectionProfile.portHopping,
            interval = connectionProfile.portHoppingInterval
              ?.trim()
              ?.toIntOrNull()
              ?.takeIf { it >= 5 }
              ?: 30
          )
        }
        streamSettings.hysteriaSettings = hysteriaSetting
      }
    }
    return sni
  }

  /**
   * Configures TLS or REALITY security settings for an outbound connection.
   *
   * Sets up security-related parameters like certificates, fingerprints, and SNI.
   *
   * @param streamSettings The stream settings to configure
   * @param connectionProfile The profile containing security configuration
   * @param sniExt An external SNI value to use if the profile doesn't specify one
   */
  fun populateTlsSettings(
    streamSettings: StreamSettingsBean,
    connectionProfile: ConnectionProfile,
    sniExt: String?,
  ) {
    val streamSecurity = connectionProfile.security.orEmpty()
    val allowInsecure = connectionProfile.insecure == true
    val sni = if (connectionProfile.sni.isNullOrEmpty()) {
      when {
        sniExt.isNotNullEmpty() && Utils.isDomainName(sniExt) -> sniExt
        connectionProfile.server.isNotNullEmpty() && Utils.isDomainName(connectionProfile.server) -> connectionProfile.server
        else -> sniExt
      }
    } else {
      connectionProfile.sni
    }

    streamSettings.security = streamSecurity.nullIfBlank()
    if (streamSettings.security == null) return
    val realityPk = connectionProfile.publicKey.nullIfBlank()
    val tlsSetting = StreamSettingsBean.TlsSettingsBean(
      allowInsecure = allowInsecure,
      serverName = sni.nullIfBlank(),
      fingerprint = connectionProfile.fingerPrint.nullIfBlank(),
      alpn = connectionProfile.alpn?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
        .takeIf { !it.isNullOrEmpty() },
      echConfigList = connectionProfile.echConfigList.nullIfBlank(),
      echForceQuery = connectionProfile.echForceQuery.nullIfBlank(),
      pinnedPeerCertSha256 = connectionProfile.pinnedCA256.nullIfBlank(),
      publicKey = realityPk,
      realityPublicKeyPassword = realityPk,
      shortId = connectionProfile.shortId.nullIfBlank(),
      spiderX = connectionProfile.spiderX.nullIfBlank(),
      mldsa65Verify = connectionProfile.mldsa65Verify.nullIfBlank(),
    )
    if (streamSettings.security == AppConfig.TLS) {
      streamSettings.tlsSettings = tlsSetting
      streamSettings.realitySettings = null
    } else if (streamSettings.security == AppConfig.REALITY) {
      streamSettings.tlsSettings = null
      streamSettings.realitySettings = tlsSetting
    }
  }

  //endregion
}
