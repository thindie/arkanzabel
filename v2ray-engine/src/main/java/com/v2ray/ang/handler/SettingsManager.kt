package com.v2ray.ang.handler

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.google.gson.JsonSyntaxException
import com.v2ray.ang.AppConfig
import com.v2ray.ang.AppConfig.GEOIP_PRIVATE
import com.v2ray.ang.AppConfig.GEOSITE_PRIVATE
import com.v2ray.ang.AppConfig.TAG_DIRECT
import com.v2ray.ang.AppConfig.VPN
import com.v2ray.ang.dto.ProfileItem
import com.v2ray.ang.dto.RulesetItem
import com.v2ray.ang.dto.V2rayConfig
import com.v2ray.ang.enums.Protocol
import com.v2ray.ang.enums.Language
import com.v2ray.ang.enums.RoutingType
import com.v2ray.ang.enums.VpnInterfaceAddressConfig
import com.v2ray.ang.util.JsonUtil
import com.v2ray.ang.util.Utils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Collections
import java.util.Locale

object SettingsManager {

  fun initRoutingRulesets(context: Context) {
    val exist = KeyValueStorage.decodeRoutingRulesets()
    if (exist.isNullOrEmpty()) {
      val rulesetList = getPresetRoutingRulesets(context) ?: return
      KeyValueStorage.encodeRoutingRulesets(rulesetList)
    }
  }

  private fun getPresetRoutingRulesets(context: Context, index: Int = 0): List<RulesetItem>? {
    val fileName = RoutingType.fromIndex(index).fileName
    val assets = Utils.readTextFromAssets(context, fileName)
    if (assets.isEmpty()) return null
    return JsonUtil.fromJson(assets, Array<RulesetItem>::class.java)?.toList()
  }

  fun resetRoutingRulesetsFromPresets(context: Context, index: Int) {
    val rulesetList = getPresetRoutingRulesets(context, index) ?: return
    resetRoutingRulesetsCommon(rulesetList.toMutableList())
  }

  fun resetRoutingRulesets(content: String?): Boolean {
    if (content.isNullOrEmpty()) return false
    return try {
      val rulesetList =
        JsonUtil.fromJson(content, Array<RulesetItem>::class.java)?.toList() ?: return false
      if (rulesetList.isEmpty()) return false
      resetRoutingRulesetsCommon(rulesetList.toMutableList())
      true
    } catch (e: JsonSyntaxException) {
      Log.e(AppConfig.TAG, "Failed to reset routing rulesets", e)
      false
    }
  }

  private fun resetRoutingRulesetsCommon(rulesetList: MutableList<RulesetItem>) {
    val rulesetNew = mutableListOf<RulesetItem>()
    KeyValueStorage.decodeRoutingRulesets()?.forEach { key ->
      if (key.locked == true) {
        rulesetNew.add(key)
      }
    }
    rulesetNew.addAll(rulesetList)
    KeyValueStorage.encodeRoutingRulesets(rulesetNew)
  }

  fun getRoutingRuleset(index: Int): RulesetItem? {
    if (index < 0) return null
    val rulesetList = KeyValueStorage.decodeRoutingRulesets()
    if (rulesetList.isNullOrEmpty()) return null
    return rulesetList[index]
  }

  fun saveRoutingRuleset(index: Int, ruleset: RulesetItem?) {
    if (ruleset == null) return
    val rulesetList = KeyValueStorage.decodeRoutingRulesets()?.toMutableList() ?: mutableListOf()
    if (index < 0 || index >= rulesetList.size) {
      rulesetList.add(0, ruleset)
    } else {
      rulesetList[index] = ruleset
    }
    KeyValueStorage.encodeRoutingRulesets(rulesetList)
  }

  fun removeRoutingRuleset(index: Int) {
    if (index < 0) return
    val rulesetList = KeyValueStorage.decodeRoutingRulesets()?.toMutableList() ?: return
    if (rulesetList.isEmpty()) return
    rulesetList.removeAt(index)
    KeyValueStorage.encodeRoutingRulesets(rulesetList.toList())
  }

  fun routingRulesetsBypassLan(): Boolean {
    val vpnBypassLan = KeyValueStorage.decodeSettingsString(AppConfig.PREF_VPN_BYPASS_LAN) ?: "1"
    if (vpnBypassLan == "1") return true
    if (vpnBypassLan == "2") return false

    val guid = KeyValueStorage.getSelectServer() ?: return false
    val config = KeyValueStorage.decodeServerConfig(guid) ?: return false
    if (config.protocol == Protocol.Custom) {
      val raw = KeyValueStorage.decodeServerRaw(guid) ?: return false
      val v2rayConfig = JsonUtil.fromJson(raw, V2rayConfig::class.java)
      val exist = v2rayConfig?.routing?.rules?.filter { it.outboundTag == TAG_DIRECT }?.any {
        it.domain?.contains(GEOSITE_PRIVATE) == true || it.ip?.contains(GEOIP_PRIVATE) == true
      }
      return exist == true
    }

    val rulesetItems = KeyValueStorage.decodeRoutingRulesets()
    val exist = rulesetItems?.filter { it.enabled && it.outboundTag == TAG_DIRECT }?.any {
      it.domain?.contains(GEOSITE_PRIVATE) == true || it.ip?.contains(GEOIP_PRIVATE) == true
    }
    return exist == true
  }

  fun swapRoutingRuleset(fromPosition: Int, toPosition: Int) {
    val rulesetList = KeyValueStorage.decodeRoutingRulesets()?.toMutableList() ?: return
    if (rulesetList.isEmpty()) return
    Collections.swap(rulesetList, fromPosition, toPosition)
    KeyValueStorage.encodeRoutingRulesets(rulesetList)
  }

  fun swapSubscriptions(fromPosition: Int, toPosition: Int) {
    val subsList = KeyValueStorage.decodeSubsList().toMutableList()
    if (subsList.isEmpty()) return
    Collections.swap(subsList, fromPosition, toPosition)
    KeyValueStorage.encodeSubsList(subsList.toList())
  }

  fun getServerViaRemarks(remarks: String?): ProfileItem? {
    if (remarks.isNullOrEmpty()) return null
    for (guid in KeyValueStorage.decodeServerList()) {
      val profile = KeyValueStorage.decodeServerConfig(guid)
      if (profile != null && profile.remarks == remarks) {
        return profile
      }
    }
    return null
  }

  fun getSocksPort(): Int =
    KeyValueStorage.decodeSettingsStringAsInt(
      AppConfig.PREF_SOCKS_PORT,
      AppConfig.PORT_SOCKS.toInt(),
    )

  fun getHttpPort(): Int {
    return getSocksPort() + if (Utils.isXray()) 0 else 1
  }

  fun initAssets(context: Context, assets: AssetManager) {
    val extFolder = Utils.userAssetPath(context)
    try {
      val geo = arrayOf("geosite.dat", "geoip.dat")
      assets.list("")
        ?.filter { geo.contains(it) }
        ?.filter { !File(extFolder, it).exists() }
        ?.forEach { name ->
          val target = File(extFolder, name)
          assets.open(name).use { input ->
            FileOutputStream(target).use { output ->
              input.copyTo(output)
            }
          }
          Log.i(AppConfig.TAG, "Copied from apk assets folder to ${target.absolutePath}")
        }
    } catch (e: IOException) {
      Log.e(AppConfig.TAG, "asset copy failed", e)
    }
  }

  fun getDomesticDnsServers(): List<String> {
    val domesticDns =
      KeyValueStorage.decodeSettingsString(AppConfig.PREF_DOMESTIC_DNS) ?: AppConfig.DNS_DIRECT
    val ret =
      domesticDns.split(",").filter { Utils.isPureIpAddress(it) || Utils.isCoreDNSAddress(it) }
    return ret.ifEmpty { listOf(AppConfig.DNS_DIRECT) }
  }

  fun getRemoteDnsServers(): List<String> {
    val remoteDns =
      KeyValueStorage.decodeSettingsString(AppConfig.PREF_REMOTE_DNS) ?: AppConfig.DNS_PROXY
    val ret =
      remoteDns.split(",").filter { Utils.isPureIpAddress(it) || Utils.isCoreDNSAddress(it) }
    return ret.ifEmpty { listOf(AppConfig.DNS_PROXY) }
  }

  fun getVpnDnsServers(): List<String> {
    val vpnDns = KeyValueStorage.decodeSettingsString(AppConfig.PREF_VPN_DNS) ?: AppConfig.DNS_VPN
    return vpnDns.split(",").filter { Utils.isPureIpAddress(it) }
  }

  fun getDelayTestUrl(second: Boolean = false): String {
    return if (second) {
      AppConfig.DELAY_TEST_URL2
    } else {
      KeyValueStorage.decodeSettingsString(AppConfig.PREF_DELAY_TEST_URL)
        ?: AppConfig.DELAY_TEST_URL
    }
  }

  fun getLocale(): Locale {
    val langCode =
      KeyValueStorage.decodeSettingsString(AppConfig.PREF_LANGUAGE) ?: Language.AUTO.code
    val language = Language.fromCode(langCode)

    return when (language) {
      Language.AUTO -> Utils.getSysLocale()
      Language.ENGLISH -> Locale.ENGLISH
      Language.CHINA -> Locale.CHINA
      Language.TRADITIONAL_CHINESE -> Locale.TRADITIONAL_CHINESE
      Language.VIETNAMESE -> Locale.forLanguageTag("vi")
      Language.RUSSIAN -> Locale.forLanguageTag("ru")
      Language.PERSIAN -> Locale.forLanguageTag("fa")
      Language.ARABIC -> Locale.forLanguageTag("ar")
      Language.BANGLA -> Locale.forLanguageTag("bn")
      Language.BAKHTIARI -> Locale.forLanguageTag("bqi-IR")
    }
  }

  fun setNightMode() {
    when (KeyValueStorage.decodeSettingsString(AppConfig.PREF_UI_MODE_NIGHT, "0")) {
      "0" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
      "1" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
      "2" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
    }
  }

  fun getCurrentVpnInterfaceAddressConfig(): VpnInterfaceAddressConfig {
    val selectedIndex =
      KeyValueStorage.decodeSettingsString(AppConfig.PREF_VPN_INTERFACE_ADDRESS_CONFIG_INDEX, "0")
        ?.toInt()
    return VpnInterfaceAddressConfig.getConfigByIndex(selectedIndex ?: 0)
  }

  fun getVpnMtu(): Int =
    KeyValueStorage.decodeSettingsStringAsInt(AppConfig.PREF_VPN_MTU, AppConfig.VPN_MTU)

  fun isUsingHevTun(): Boolean {
    // Default off: hev requires bundled `libhev-socks5-tunnel.so`; without it use TUN fd → core.
    return KeyValueStorage.decodeSettingsBool(AppConfig.PREF_USE_HEV_TUNNEL, false)
  }

  fun isVpnMode(): Boolean {
    val mode = KeyValueStorage.decodeSettingsString(AppConfig.PREF_MODE) ?: VPN
    return mode == VPN
  }

  fun ensureDefaultSettings() {
    ensureDefaultValue(AppConfig.PREF_MODE, VPN)
    ensureDefaultValue(AppConfig.PREF_VPN_DNS, AppConfig.DNS_VPN)
    ensureDefaultValue(AppConfig.PREF_VPN_MTU, AppConfig.VPN_MTU.toString())
    ensureDefaultValue(
      AppConfig.SUBSCRIPTION_AUTO_UPDATE_INTERVAL,
      AppConfig.SUBSCRIPTION_DEFAULT_UPDATE_INTERVAL
    )
    ensureDefaultValue(AppConfig.PREF_SOCKS_PORT, AppConfig.PORT_SOCKS)
    ensureDefaultValue(AppConfig.PREF_REMOTE_DNS, AppConfig.DNS_PROXY)
    ensureDefaultValue(AppConfig.PREF_DOMESTIC_DNS, AppConfig.DNS_DIRECT)
    ensureDefaultValue(AppConfig.PREF_DELAY_TEST_URL, AppConfig.DELAY_TEST_URL)
    ensureDefaultValue(AppConfig.PREF_IP_API_URL, AppConfig.IP_API_URL)
    ensureDefaultValue(AppConfig.PREF_HEV_TUNNEL_RW_TIMEOUT, AppConfig.HEVTUN_RW_TIMEOUT)
    ensureDefaultValue(AppConfig.PREF_MUX_CONCURRENCY, "8")
    ensureDefaultValue(AppConfig.PREF_MUX_XUDP_CONCURRENCY, "8")
    ensureDefaultValue(AppConfig.PREF_FRAGMENT_LENGTH, "50-100")
    ensureDefaultValue(AppConfig.PREF_FRAGMENT_INTERVAL, "10-20")
  }

  private fun ensureDefaultValue(key: String, default: String) {
    if (KeyValueStorage.decodeSettingsString(key).isNullOrEmpty()) {
      KeyValueStorage.encodeSettings(key, default)
    }
  }

  fun migrateHysteria2PinSHA256() {
    val migrationKey = "hysteria2_pin_sha256_migrated"
    if (KeyValueStorage.decodeSettingsBool(migrationKey, false)) {
      return
    }
    for (guid in KeyValueStorage.decodeServerList()) {
      val profile = KeyValueStorage.decodeServerConfig(guid) ?: continue
      if (profile.protocol != Protocol.Hysteria2) continue
      if (profile.pinSHA256.isNullOrEmpty() || !profile.pinnedCA256.isNullOrEmpty()) continue
      profile.pinnedCA256 = profile.pinSHA256
      profile.pinSHA256 = null
      KeyValueStorage.encodeServerConfig(guid, profile)
    }
    KeyValueStorage.encodeSettings(migrationKey, true)
  }
}
