package com.v2ray.ang.runtime

import android.content.Context
import android.text.TextUtils
import android.util.Log
import com.v2ray.ang.runtimebuilder.ConfigAssembler
import com.v2ray.ang.runtimebuilder.ConnectionProfileToOutboundMapper
import com.v2ray.ang.runtimebuilder.DnsConfigStep
import com.v2ray.ang.runtimebuilder.DomainResolveStep
import com.v2ray.ang.runtimebuilder.InboundConfigStep
import com.v2ray.ang.runtimebuilder.OutboundConfigStep
import com.v2ray.ang.runtimebuilder.RoutingConfigStep
import com.google.gson.JsonArray
import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.ConnectionProfile
import com.v2ray.ang.dto.V2Ray
import com.v2ray.ang.dto.V2rayConfig
import com.v2ray.ang.dto.V2rayConfig.Outbound
import com.v2ray.ang.dto.V2rayConfig.Outbound.OutSettings
import com.v2ray.ang.dto.V2rayConfig.Outbound.StreamSettings
import com.v2ray.ang.dto.V2rayConfig.Routing.Rules
import com.v2ray.ang.error.AssetConfigMissingError
import com.v2ray.ang.error.ConfigSerializationError
import com.v2ray.ang.error.ConfigValidationError
import com.v2ray.ang.error.ProfileNotFoundError
import com.v2ray.ang.error.RoutingConfigError
import com.v2ray.ang.error.StoredRawMissingError
import com.v2ray.ang.enums.NetworkType
import com.v2ray.ang.enums.Protocol
import com.v2ray.ang.extension.isNotNullEmpty
import com.v2ray.ang.extension.nullIfBlank
import com.v2ray.ang.util.JsonUtil
import com.v2ray.ang.util.Utils
import java.util.regex.PatternSyntaxException

object V2rayConfigManager {
  private var initConfigCache: String? = null
  private var initConfigCacheWithTun: String? = null
  private val inboundConfigStep by lazy { InboundConfigStep(::needTun) }
  private val routingConfigStep by lazy { RoutingConfigStep() }
  private val dnsConfigStep by lazy { DnsConfigStep(::getUserRule2Domain) }
  private val outboundConfigStep by lazy { OutboundConfigStep(ConnectionProfileToOutboundMapper::map) }
  private val domainResolveStep by lazy { DomainResolveStep() }
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
   * Builds core JSON for the given profile [guid].
   *
   * Throws [com.v2ray.ang.error.AppError] (or other [RuntimeException]) on failure; callers map to UI.
   */
  fun getV2rayConfig(context: Context, guid: String): V2Ray {
    val config = KeyValueStorage.decodeServerConfig(guid) ?: throw ProfileNotFoundError(guid)
    return when (config.protocol) {
      Protocol.Custom -> getV2rayCustomConfig(context, guid)
      Protocol.PolicyGroup -> getV2rayGroupConfig(context, guid, config)
      else -> getV2rayNormalConfig(context, guid, config)
    }
  }

  /**
   * Same as [getV2rayConfig] but tuned for outbound delay measurement.
   */
  fun getV2rayConfig4Speedtest(context: Context, guid: String): V2Ray {
    val config = KeyValueStorage.decodeServerConfig(guid) ?: throw ProfileNotFoundError(guid)
    return when (config.protocol) {
      Protocol.Custom -> getV2rayCustomConfig(context, guid)
      Protocol.PolicyGroup -> getV2rayGroupConfig(context, guid, config)
      else -> getV2rayNormalConfig4Speedtest(context, guid, config)
    }
  }

  private fun getV2rayCustomConfig(
    context: Context,
    guid: String,
  ): V2Ray {
    val raw = KeyValueStorage.decodeServerRaw(guid) ?: throw StoredRawMissingError(guid)
    if (!needTun()) {
      return V2Ray(guid, raw)
    }

    val json = JsonUtil.parseString(raw) ?: return V2Ray(guid, raw)
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
        if (tag == "tun") return V2Ray(guid, raw)
      }
    }

    val templateConfig = initV2rayConfig(context)
    val inboundTun = templateConfig.inbounds.firstOrNull { it.tag == "tun" }
      ?: throw ConfigValidationError(
        message = "Template has no tun inbound",
        userReadable = "VPN template is missing TUN settings",
        stage = "customTun",
      )
    inboundTun.settings?.mtu = SettingsManager.getVpnMtu()

    inboundsJson.add(JsonUtil.parseString(JsonUtil.toJson(inboundTun)))
    if (inboundsJson.size() == 1) {
      json.add("inbounds", inboundsJson)
    }

    val updatedRaw = JsonUtil.toJsonPretty(json) ?: throw ConfigSerializationError("customWithTun")
    return V2Ray(guid, updatedRaw)
  }

  private fun getV2rayGroupConfig(
    context: Context,
    guid: String,
    config: ConnectionProfile,
  ): V2Ray {
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
          } catch (invalidRegex: PatternSyntaxException) {
            profile.remarks.contains(filter)
          }
        }
      }

    val v2rayConfig = getV2rayMultipleConfig(context, config, configList)
    return V2Ray(guid, v2RayJson(v2rayConfig, "policyGroup"))
  }

  private fun getV2rayNormalConfig(
    context: Context,
    guid: String,
    config: ConnectionProfile,
  ): V2Ray {
    val address = config.server ?: throw ConfigValidationError(
      message = "Server address is missing",
      userReadable = "Server address is missing",
      extras = mapOf("guid" to guid),
    )
    if (!Utils.isPureIpAddress(address) && !Utils.isValidUrl(address)) {
      throw ConfigValidationError(
        message = "$address is an invalid ip or domain",
        userReadable = "Invalid server address",
        extras = mapOf("address" to address),
      )
    }

    val v2rayConfig = initV2rayConfig(context)
    v2rayConfig.log.loglevel =
      KeyValueStorage.decodeSettingsString(AppConfig.PREF_LOGLEVEL) ?: "warning"
    v2rayConfig.remarks = config.remarks

    val assembled = configAssembler.applyStandardSteps(v2rayConfig, config)
    if (assembled.getProxyOutbound() == null) {
      throw ConfigValidationError(
        message = "No proxy outbound after assembly",
        userReadable = "Could not build proxy settings",
        stage = "assemble",
        extras = mapOf("guid" to guid),
      )
    }
    return V2Ray(guid, v2RayJson(assembled, "normal"))
  }

  private fun getV2rayMultipleConfig(
    context: Context,
    config: ConnectionProfile,
    configList: List<ConnectionProfile>,
  ): V2rayConfig {
    val validConfigs = configList.asSequence().filter { it.server.isNotNullEmpty() }
      .filter { !Utils.isPureIpAddress(it.server!!) || Utils.isValidUrl(it.server!!) }
      .filter { it.protocol != Protocol.Custom }
      .filter { it.protocol != Protocol.PolicyGroup }
      .toList()

    if (validConfigs.isEmpty()) {
      throw ConfigValidationError(
        message = "All configs are invalid for policy group",
        userReadable = "No valid servers in this group",
        stage = "policyGroup",
      )
    }

    val initialConfig = initV2rayConfig(context)
    initialConfig.log.loglevel =
      KeyValueStorage.decodeSettingsString(AppConfig.PREF_LOGLEVEL) ?: "warning"
    initialConfig.remarks = config.remarks

    val configWithInbounds = getInbounds(initialConfig)

    configWithInbounds.outbounds.removeAt(0)
    val outboundsList = mutableListOf<Outbound>()
    var index = 0
    for (connectionProfile in validConfigs) {
      index++
      val outbound = ConnectionProfileToOutboundMapper.map(connectionProfile) ?: continue
      val ret = updateOutboundWithGlobalSettings(outbound)
      if (!ret) continue
      outbound.tag = "proxy-$index"
      outboundsList.add(outbound)
    }
    outboundsList.addAll(configWithInbounds.outbounds)
    configWithInbounds.outbounds = ArrayList(outboundsList)

    return configWithInbounds
      .then { getRouting(it) }
      .then { getFakeDns(it) }
      .then { getDns(it) }
      .then {
        getBalance(it, config)
        it
      }
      .then {
        if (KeyValueStorage.decodeSettingsBool(AppConfig.PREF_LOCAL_DNS_ENABLED)) {
          getCustomLocalDns(it)
        } else {
          it
        }
      }
      .then { it.applySpeedPolicyToggles() }
      .then { it.applyOptionalDomainResolve() }
  }

  private fun getV2rayNormalConfig4Speedtest(
    context: Context,
    guid: String,
    config: ConnectionProfile,
  ): V2Ray {
    val address = config.server ?: throw ConfigValidationError(
      message = "Server address is missing",
      userReadable = "Server address is missing",
      extras = mapOf("guid" to guid),
    )
    if (!Utils.isPureIpAddress(address) && !Utils.isValidUrl(address)) {
      throw ConfigValidationError(
        message = "$address is an invalid ip or domain",
        userReadable = "Invalid server address",
        extras = mapOf("address" to address),
      )
    }

    val initialConfig = initV2rayConfig(context)
    val speedtestConfig = initialConfig
      .then { getOutbounds(it, config) }
      .then { getMoreOutbounds(it, config.subscriptionId) }
      .then { it.invalidateForSpeedtest() }

    return V2Ray(guid, v2RayJson(speedtestConfig, "speedtest"))
  }

  /**
   * Loads the JSON template from assets (cached). Throws [AssetConfigMissingError] if empty,
   * [ConfigValidationError] if parsing fails.
   */
  private fun initV2rayConfig(context: Context): V2rayConfig {
    val assets: String
    if (needTun()) {
      val name = "v2ray_config_with_tun.json"
      assets = initConfigCacheWithTun ?: Utils.readTextFromAssets(context, name)
      if (TextUtils.isEmpty(assets)) {
        throw AssetConfigMissingError(name)
      }
      initConfigCacheWithTun = assets
    } else {
      val name = "v2ray_config.json"
      assets = initConfigCache ?: Utils.readTextFromAssets(context, name)
      if (TextUtils.isEmpty(assets)) {
        throw AssetConfigMissingError(name)
      }
      initConfigCache = assets
    }
    return JsonUtil.fromJson(assets, V2rayConfig::class.java)
      ?: throw ConfigValidationError(
        message = "Failed to parse JSON template",
        userReadable = "Invalid VPN template",
        stage = "parseTemplate",
      )
  }

  private fun v2RayJson(model: V2rayConfig, stage: String): String {
    return JsonUtil.toJsonPretty(model)?.takeIf { it.isNotEmpty() }
      ?: throw ConfigSerializationError(stage)
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
   * @return Updated config, or null if inbound configuration fails.
   */
  private fun getInbounds(v2rayConfig: V2rayConfig): V2rayConfig {
    return inboundConfigStep.applyInbounds(v2rayConfig)
  }

  /**
   * Configures the fake DNS settings if enabled.
   *
   * Adds FakeDNS configuration to v2rayConfig if both local DNS and fake DNS are enabled.
   *
   * @param v2rayConfig The V2ray configuration object to be modified
   */
  private fun getFakeDns(v2rayConfig: V2rayConfig): V2rayConfig {
    return dnsConfigStep.applyFakeDns(v2rayConfig)
  }

  /**
   * Configures routing settings for V2ray.
   *
   * Sets up the domain strategy and adds routing rules from saved rulesets.
   *
   * @param v2rayConfig The V2ray configuration object to be modified
   * @return Updated config, or null if routing configuration fails.
   */
  private fun getRouting(v2rayConfig: V2rayConfig): V2rayConfig {
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
   * @return Updated config, or null if local DNS configuration fails.
   */
  private fun getCustomLocalDns(v2rayConfig: V2rayConfig): V2rayConfig {
    return dnsConfigStep.applyCustomLocalDns(v2rayConfig)
  }

  /**
   * Configures the DNS settings for V2ray.
   *
   * Sets up DNS servers, hosts, and routing rules for DNS resolution.
   *
   * @param v2rayConfig The V2ray configuration object to be modified
   * @return Updated config, or null if DNS configuration fails.
   */
  private fun getDns(v2rayConfig: V2rayConfig): V2rayConfig {
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
   * @param connectionProfile The connection profile containing connection details
   * @return Updated config, or null if outbound configuration fails.
   */
  private fun getOutbounds(v2rayConfig: V2rayConfig, connectionProfile: ConnectionProfile): V2rayConfig {
    return outboundConfigStep.applyOutbounds(v2rayConfig, connectionProfile)
  }

  /**
   * Configures additional outbound connections for proxy chaining.
   *
   * Sets up previous and next proxies in a subscription for advanced routing capabilities.
   *
   * @param v2rayConfig The V2ray configuration object to be modified
   * @param subscriptionId The subscription ID to look up related proxies
   * @return Updated config. If additional outbounds cannot be applied, returns original config.
   */
  private fun getMoreOutbounds(v2rayConfig: V2rayConfig, subscriptionId: String): V2rayConfig {
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
  private fun updateOutboundWithGlobalSettings(outbound: Outbound): Boolean {
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
          val balancer = V2rayConfig.Routing.Balancer(
            tag = AppConfig.TAG_BALANCER,
            selector = lstSelector,
            strategy = V2rayConfig.Routing.StrategyObject(
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
          val balancer = V2rayConfig.Routing.Balancer(
            tag = AppConfig.TAG_BALANCER,
            selector = lstSelector,
            strategy = V2rayConfig.Routing.StrategyObject(
              type = "random"
            )
          )
          v2rayConfig.routing.balancers = listOf(balancer)
        }

        "3" -> {
          // Round Robin
          val balancer = V2rayConfig.Routing.Balancer(
            tag = AppConfig.TAG_BALANCER,
            selector = lstSelector,
            strategy = V2rayConfig.Routing.StrategyObject(
              type = "roundRobin"
            )
          )
          v2rayConfig.routing.balancers = listOf(balancer)
        }

        else -> {
          // Default: Least Ping
          val balancer = V2rayConfig.Routing.Balancer(
            tag = AppConfig.TAG_BALANCER,
            selector = lstSelector,
            strategy = V2rayConfig.Routing.StrategyObject(
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
          Rules(
            ip = arrayListOf("0.0.0.0/0", "::/0"),
            balancerTag = AppConfig.TAG_BALANCER,
          )
        )
      } else {
        v2rayConfig.routing.rules.add(
          Rules(
            network = "tcp,udp",
            balancerTag = AppConfig.TAG_BALANCER,
          )
        )
      }
    } catch (runtime: RuntimeException) {
      Log.e(AppConfig.TAG, "Failed to configure balance", runtime)
      throw RoutingConfigError(
        message = "Failed to configure balance for policy group",
        source = "V2rayConfigManager.getBalance",
        cause = runtime
      )
    }
  }

  /**
   * Updates the outbound with fragment settings for traffic optimization.
   *
   * Configures packet fragmentation for TLS and REALITY protocols if enabled.
   *
   * @param v2rayConfig The V2ray configuration object to be modified
   * @return Updated config, or null if fragment configuration fails.
   */
  private fun updateOutboundFragment(v2rayConfig: V2rayConfig): V2rayConfig? {
    return outboundConfigStep.applyOutboundFragment(v2rayConfig)
  }

  /**
   * Resolves domain names to IP addresses in outbound connections.
   *
   * Pre-resolves domains to improve connection speed and reliability.
   *
   * @param v2rayConfig The V2ray configuration object to be modified
   */
  private fun resolveOutboundDomainsToHosts(v2rayConfig: V2rayConfig): V2rayConfig {
    return domainResolveStep.resolveOutboundDomainsToHosts(v2rayConfig)
  }

  private fun V2rayConfig.invalidateForSpeedtest(): V2rayConfig {
    log.loglevel = KeyValueStorage.decodeSettingsString(AppConfig.PREF_LOGLEVEL) ?: "warning"
    inbounds.clear()
    routing.rules.clear()
    dns = null
    fakedns = null
    stats = null
    policy = null
    outbounds.forEach { outbound -> outbound.mux = null }
    return this
  }

  private fun V2rayConfig.applySpeedPolicyToggles(): V2rayConfig {
    if (!KeyValueStorage.decodeSettingsBool(AppConfig.PREF_SPEED_ENABLED)) {
      stats = null
      policy = null
    }
    return this
  }

  private fun V2rayConfig.applyOptionalDomainResolve(): V2rayConfig {
    if (KeyValueStorage.decodeSettingsString(AppConfig.PREF_OUTBOUND_DOMAIN_RESOLVE_METHOD, "1") == "1") {
      resolveOutboundDomainsToHosts(this)
    }
    return this
  }

  private inline fun V2rayConfig.then(step: (V2rayConfig) -> V2rayConfig): V2rayConfig = step(this)


  /**
   * Creates an initial outbound configuration for a specific protocol type.
   *
   * Provides a template configuration for different protocol types.
   *
   * @param configType The type of configuration to create
   * @return An initial Outbound for the specified configuration type, or null for custom types
   */
  fun createInitOutbound(configType: Protocol): Outbound? {
    return when (configType) {
      Protocol.Vmess,
      Protocol.Vless,
        ->
        return Outbound(
          protocol = configType.name.lowercase(),
          settings = OutSettings(
            vnext = listOf(
              OutSettings.Vnext(
                users = listOf(OutSettings.Vnext.Users())
              )
            )
          ),
          streamSettings = StreamSettings()
        )

      Protocol.ShadowSocks,
      Protocol.Socks,
      Protocol.Http,
      Protocol.Trojan,
        ->
        return Outbound(
          protocol = configType.name.lowercase(),
          settings = OutSettings(
            servers = listOf(OutSettings.Servers())
          ),
          streamSettings = StreamSettings()
        )

      Protocol.WireGuard ->
        return Outbound(
          protocol = configType.name.lowercase(),
          settings = OutSettings(
            secretKey = "",
            peers = listOf(OutSettings.WireGuard())
          )
        )

      Protocol.Hysteria,
      Protocol.Hysteria2,
        ->
        return Outbound(
          protocol = Protocol.Hysteria.name.lowercase(),
          settings = OutSettings(
            servers = null
          ),
          streamSettings = StreamSettings()
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
    streamSettings: StreamSettings,
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
        val tcpSetting = StreamSettings.TcpSettings()
        if (headerType == AppConfig.HEADER_TYPE_HTTP) {
          tcpSetting.header.type = AppConfig.HEADER_TYPE_HTTP
          if (!TextUtils.isEmpty(host) || !TextUtils.isEmpty(path)) {
            val requestObj = StreamSettings.TcpSettings.Header.Request()
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
        streamSettings.kcpSettings = StreamSettings.KcpSettings()
        val udpMaskList = mutableListOf<StreamSettings.FinalMask.Mask>()
        if (!headerType.isNullOrEmpty() && headerType != "none") {
          val kcpHeaderType = when {
            headerType == "wechat-video" -> "header-wechat"
            else -> "header-$headerType"
          }
          udpMaskList.add(
            StreamSettings.FinalMask.Mask(
              type = kcpHeaderType,
              settings = if (headerType == "dns" && !host.isNullOrEmpty()) {
                StreamSettings.FinalMask.Mask.MaskSettings(
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
            StreamSettings.FinalMask.Mask(
              type = "mkcp-original"
            )
          )
        } else {
          udpMaskList.add(
            StreamSettings.FinalMask.Mask(
              type = "mkcp-aes128gcm",
              settings = StreamSettings.FinalMask.Mask.MaskSettings(
                password = seed
              )
            )
          )
        }
        streamSettings.finalmask = StreamSettings.FinalMask(
          udp = udpMaskList.toList()
        )
      }

      NetworkType.WS.type -> {
        val wssetting = StreamSettings.WsSettings()
        wssetting.headers.Host = host.orEmpty()
        sni = host
        wssetting.path = path ?: "/"
        streamSettings.wsSettings = wssetting
      }

      NetworkType.HTTP_UPGRADE.type -> {
        val httpupgradeSetting = StreamSettings.HttpupgradeSettings()
        httpupgradeSetting.host = host.orEmpty()
        sni = host
        httpupgradeSetting.path = path ?: "/"
        streamSettings.httpupgradeSettings = httpupgradeSetting
      }

      NetworkType.XHTTP.type -> {
        val xhttpSetting = StreamSettings.XhttpSettings()
        xhttpSetting.host = host.orEmpty()
        sni = host
        xhttpSetting.path = path ?: "/"
        xhttpSetting.mode = xhttpMode
        xhttpSetting.extra = JsonUtil.parseString(xhttpExtra.orEmpty())
        streamSettings.xhttpSettings = xhttpSetting
      }

      NetworkType.H2.type, NetworkType.HTTP.type -> {
        streamSettings.network = NetworkType.H2.type
        val h2Setting = StreamSettings.HttpSettings()
        h2Setting.host = host.orEmpty().split(",").map { it.trim() }.filter { it.isNotEmpty() }
        sni = h2Setting.host.getOrNull(0)
        h2Setting.path = path ?: "/"
        streamSettings.httpSettings = h2Setting
      }

//                    "quic" -> {
//                        val quicsetting = QuicSetting()
//                        quicsetting.security = quicSecurity ?: "none"
//                        quicsetting.key = key.orEmpty()
//                        quicsetting.header.type = headerType ?: "none"
//                        quicSettings = quicsetting
//                    }

      NetworkType.GRPC.type -> {
        val grpcSetting = StreamSettings.GrpcSettings()
        grpcSetting.multiMode = mode == "multi"
        grpcSetting.serviceName = serviceName.orEmpty()
        grpcSetting.authority = authority.orEmpty()
        grpcSetting.idle_timeout = 60
        grpcSetting.health_check_timeout = 20
        sni = authority
        streamSettings.grpcSettings = grpcSetting
      }

      NetworkType.HYSTERIA.type -> {
        val hysteriaSetting = StreamSettings.HysteriaSettings(
          version = 2,
          auth = connectionProfile.password.orEmpty(),
          up = connectionProfile.bandwidthUp?.ifEmpty { "0" }.orEmpty(),
          down = connectionProfile.bandwidthDown?.ifEmpty { "0" }.orEmpty(),
          udphop = null
        )
        if (connectionProfile.portHopping.isNotNullEmpty()) {
          hysteriaSetting.udphop = StreamSettings.HysteriaSettings.HysteriaUdpHop(
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
    streamSettings: StreamSettings,
    connectionProfile: ConnectionProfile,
    sniExt: String?,
  ) {
    val streamSecurity = connectionProfile.security.orEmpty()
    val allowInsecure = connectionProfile.insecure
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
    val tlsSetting = StreamSettings.TlsSettings(
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
