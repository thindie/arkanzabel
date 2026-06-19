package com.v2ray.ang.runtime

import android.content.Context
import android.util.Log
import com.google.gson.JsonIOException
import com.v2ray.ang.AppConfig
import com.tencent.mmkv.MMKV
import com.v2ray.ang.AppConfig.PREF_IS_BOOTED
import com.v2ray.ang.AppConfig.PREF_ROUTING_RULESET
import com.v2ray.ang.dto.AssetUrlCache
import com.v2ray.ang.dto.AssetUrlItem
import com.v2ray.ang.dto.ConnectionProfile
import com.v2ray.ang.dto.RulesetItem
import com.v2ray.ang.dto.ServerAffiliationInfo
import com.v2ray.ang.dto.SubscriptionCache
import com.v2ray.ang.dto.SubscriptionItem
import com.v2ray.ang.dto.WebDavConfig
import com.v2ray.ang.util.JsonUtil
import com.v2ray.ang.util.Utils

object KeyValueStorage {

    /** [IllegalStateException.message] when Gson failed to serialize a [ConnectionProfile]. */
    const val ERROR_MESSAGE_PROFILE_JSON_FAILED = "ARKNZBL_PROFILE_JSON_FAILED"

    fun initialize(context: Context) {
        MMKV.initialize(context)
    }

    private const val ID_MAIN = "MAIN"
    private const val ID_PROFILE_FULL_CONFIG = "PROFILE_FULL_CONFIG"
    private const val ID_SERVER_RAW = "SERVER_RAW"
    private const val ID_SERVER_AFF = "SERVER_AFF"
    private const val ID_SUB = "SUB"
    private const val ID_ASSET = "ASSET"
    private const val ID_SETTING = "SETTING"
    private const val KEY_SELECTED_SERVER = "SELECTED_SERVER"
    private const val KEY_ANG_CONFIGS = "ANG_CONFIGS"
    private const val KEY_SUB_IDS = "SUB_IDS"
    private const val KEY_WEBDAV_CONFIG = "WEBDAV_CONFIG"

    private val mainStorage by lazy { MMKV.mmkvWithID(ID_MAIN, MMKV.MULTI_PROCESS_MODE) }
    private val profileFullStorage by lazy {
        MMKV.mmkvWithID(
            ID_PROFILE_FULL_CONFIG,
            MMKV.MULTI_PROCESS_MODE
        )
    }
    private val serverRawStorage by lazy { MMKV.mmkvWithID(ID_SERVER_RAW, MMKV.MULTI_PROCESS_MODE) }
    private val serverAffStorage by lazy { MMKV.mmkvWithID(ID_SERVER_AFF, MMKV.MULTI_PROCESS_MODE) }
    private val subStorage by lazy { MMKV.mmkvWithID(ID_SUB, MMKV.MULTI_PROCESS_MODE) }
    private val assetStorage by lazy { MMKV.mmkvWithID(ID_ASSET, MMKV.MULTI_PROCESS_MODE) }
    private val settingsStorage by lazy { MMKV.mmkvWithID(ID_SETTING, MMKV.MULTI_PROCESS_MODE) }

    fun getSelectServer(): String? = mainStorage.decodeString(KEY_SELECTED_SERVER)

    fun setSelectServer(guid: String) {
        mainStorage.encode(KEY_SELECTED_SERVER, guid)
    }

    fun encodeServerList(serverList: List<String>) {
        mainStorage.encode(KEY_ANG_CONFIGS, JsonUtil.toJson(serverList))
    }

    fun decodeServerList(): List<String> {
        val json = mainStorage.decodeString(KEY_ANG_CONFIGS)
        return if (json.isNullOrBlank()) {
            emptyList()
        } else {
            JsonUtil.fromJson(json, Array<String>::class.java)?.toList() ?: emptyList()
        }
    }

    fun decodeServerConfig(guid: String): ConnectionProfile? {
        if (guid.isBlank()) return null
        val json = profileFullStorage.decodeString(guid)
        if (json.isNullOrBlank()) return null
        return JsonUtil.fromJson(json, ConnectionProfile::class.java)
    }

    fun encodeServerConfig(guid: String, config: ConnectionProfile): String {
        val key = when {
            guid.isNotBlank() -> guid
            else -> {
                val u = Utils.getUuid()
                if (u.isNotBlank()) u else "p${System.nanoTime()}"
            }
        }
        val json =
            try {
                JsonUtil.toJson(config)
            } catch (e: JsonIOException) {
                throw IllegalStateException(ERROR_MESSAGE_PROFILE_JSON_FAILED, e)
            }
        profileFullStorage.encode(key, json)
        val serverList = decodeServerList().toMutableList()
        if (!serverList.contains(key)) {
            serverList.add(0, key)
            encodeServerList(serverList)
            if (getSelectServer().isNullOrBlank()) {
                mainStorage.encode(KEY_SELECTED_SERVER, key)
            }
        }
        return key
    }

    fun removeServer(guid: String) {
        if (guid.isBlank()) return
        if (getSelectServer() == guid) {
            mainStorage.remove(KEY_SELECTED_SERVER)
        }
        val serverList = decodeServerList().toMutableList()
        serverList.remove(guid)
        encodeServerList(serverList)
        profileFullStorage.remove(guid)
        serverAffStorage.remove(guid)
    }

    fun removeServerViaSubid(subid: String) {
        if (subid.isBlank()) return
        profileFullStorage.allKeys()?.forEach { key ->
            decodeServerConfig(key)?.let { config ->
                if (config.subscriptionId == subid) {
                    removeServer(key)
                }
            }
        }
    }

    fun decodeServerAffiliationInfo(guid: String): ServerAffiliationInfo? {
        if (guid.isBlank()) return null
        val json = serverAffStorage.decodeString(guid)
        if (json.isNullOrBlank()) return null
        return JsonUtil.fromJson(json, ServerAffiliationInfo::class.java)
    }

    fun encodeServerTestDelayMillis(guid: String, testResult: Long) {
        if (guid.isBlank()) return
        val aff = decodeServerAffiliationInfo(guid) ?: ServerAffiliationInfo()
        aff.testDelayMillis = testResult
        serverAffStorage.encode(guid, JsonUtil.toJson(aff))
    }

    fun clearAllTestDelayResults(keys: List<String>?) {
        keys?.forEach { key ->
            decodeServerAffiliationInfo(key)?.let { aff ->
                aff.testDelayMillis = 0
                serverAffStorage.encode(key, JsonUtil.toJson(aff))
            }
        }
    }

    fun removeAllServer(): Int {
        val count = profileFullStorage.allKeys()?.count() ?: 0
        mainStorage.clearAll()
        profileFullStorage.clearAll()
        serverAffStorage.clearAll()
        return count
    }

    fun removeInvalidServer(guid: String): Int {
        var count = 0
        if (guid.isNotEmpty()) {
            decodeServerAffiliationInfo(guid)?.let { aff ->
                if (aff.testDelayMillis < 0L) {
                    removeServer(guid)
                    count++
                }
            }
        } else {
            serverAffStorage.allKeys()?.forEach { key ->
                decodeServerAffiliationInfo(key)?.let { aff ->
                    if (aff.testDelayMillis < 0L) {
                        removeServer(key)
                        count++
                    }
                }
            }
        }
        return count
    }

    fun encodeServerRaw(guid: String, config: String) {
        serverRawStorage.encode(guid, config)
    }

    fun decodeServerRaw(guid: String): String? = serverRawStorage.decodeString(guid)

    private fun initSubsList() {
        if (decodeSubsList().isNotEmpty()) return
        val subsList = subStorage.allKeys()?.toMutableList() ?: mutableListOf()
        encodeSubsList(subsList)
    }

    fun decodeSubscriptions(): List<SubscriptionCache> {
        initSubsList()
        val subscriptions = mutableListOf<SubscriptionCache>()
        decodeSubsList().forEach { key ->
            val json = subStorage.decodeString(key)
            if (!json.isNullOrBlank()) {
                val item = JsonUtil.fromJson(json, SubscriptionItem::class.java)
                if (item == null) {
                    Log.w(AppConfig.TAG, "Skip invalid subscription item: $key")
                    return@forEach
                }
                subscriptions.add(SubscriptionCache(key, item))
            }
        }
        return subscriptions
    }

    fun removeSubscription(subid: String) {
        subStorage.remove(subid)
        val subsList = decodeSubsList().toMutableList()
        subsList.remove(subid)
        encodeSubsList(subsList)
        removeServerViaSubid(subid)
    }

    fun encodeSubscription(guid: String, subItem: SubscriptionItem) {
        val key = guid.ifBlank { Utils.getUuid() }
        subStorage.encode(key, JsonUtil.toJson(subItem))
        val subsList = decodeSubsList().toMutableList()
        if (!subsList.contains(key)) {
            subsList.add(key)
            encodeSubsList(subsList)
        }
    }

    fun decodeSubscription(subscriptionId: String): SubscriptionItem? {
        val json = subStorage.decodeString(subscriptionId) ?: return null
        return JsonUtil.fromJson(json, SubscriptionItem::class.java)
    }

    fun encodeSubsList(subsList: List<String>) {
        mainStorage.encode(KEY_SUB_IDS, JsonUtil.toJson(subsList))
    }

    fun decodeSubsList(): List<String> {
        val json = mainStorage.decodeString(KEY_SUB_IDS)
        return if (json.isNullOrBlank()) {
            emptyList()
        } else {
            JsonUtil.fromJson(json, Array<String>::class.java)?.toList() ?: emptyList()
        }
    }

    fun decodeAssetUrls(): List<AssetUrlCache> {
        val assetUrlItems = mutableListOf<AssetUrlCache>()
        assetStorage.allKeys()?.forEach { key ->
            val json = assetStorage.decodeString(key)
            if (!json.isNullOrBlank()) {
                val item = JsonUtil.fromJson(json, AssetUrlItem::class.java)
                if (item == null) {
                    Log.w(AppConfig.TAG, "Skip invalid asset url item: $key")
                    return@forEach
                }
                assetUrlItems.add(AssetUrlCache(key, item))
            }
        }
        return assetUrlItems.sortedBy { it.assetUrl.addedTime }
    }

    fun removeAssetUrl(assetid: String) {
        assetStorage.remove(assetid)
    }

    fun encodeAsset(assetid: String, assetItem: AssetUrlItem) {
        val key = assetid.ifBlank { Utils.getUuid() }
        assetStorage.encode(key, JsonUtil.toJson(assetItem))
    }

    fun decodeAsset(assetid: String): AssetUrlItem? {
        val json = assetStorage.decodeString(assetid) ?: return null
        return JsonUtil.fromJson(json, AssetUrlItem::class.java)
    }

    fun decodeRoutingRulesets(): List<RulesetItem>? {
        val ruleset = settingsStorage.decodeString(PREF_ROUTING_RULESET)
        if (ruleset.isNullOrEmpty()) return null
        return JsonUtil.fromJson(ruleset, Array<RulesetItem>::class.java)?.toList() ?: emptyList()
    }

    fun encodeRoutingRulesets(rulesetList: List<RulesetItem>?) {
        if (rulesetList.isNullOrEmpty()) {
            encodeSettings(PREF_ROUTING_RULESET, "")
        } else {
            encodeSettings(PREF_ROUTING_RULESET, JsonUtil.toJson(rulesetList))
        }
    }

    fun encodeSettings(key: String, value: String?): Boolean = settingsStorage.encode(key, value)

    fun encodeSettings(key: String, value: Int): Boolean = settingsStorage.encode(key, value)

    fun encodeSettings(key: String, value: Long): Boolean = settingsStorage.encode(key, value)

    fun encodeSettings(key: String, value: Float): Boolean = settingsStorage.encode(key, value)

    fun encodeSettings(key: String, value: Boolean): Boolean = settingsStorage.encode(key, value)

    fun encodeSettings(key: String, value: Set<String>): Boolean =
        settingsStorage.encode(key, value.toMutableSet())

    fun decodeSettingsString(key: String): String? = settingsStorage.decodeString(key)

    fun decodeSettingsString(key: String, defaultValue: String?): String? =
        settingsStorage.decodeString(key, defaultValue)

    fun decodeSettingsStringAsInt(key: String, defaultValue: Int): Int =
        decodeSettingsString(key)?.toIntOrNull() ?: defaultValue

    fun decodeSettingsInt(key: String, defaultValue: Int): Int =
        settingsStorage.decodeInt(key, defaultValue)

    fun decodeSettingsLong(key: String, defaultValue: Long): Long =
        settingsStorage.decodeLong(key, defaultValue)

    fun decodeSettingsFloat(key: String, defaultValue: Float): Float =
        settingsStorage.decodeFloat(key, defaultValue)

    fun decodeSettingsBool(key: String): Boolean = settingsStorage.decodeBool(key, false)

    fun decodeSettingsBool(key: String, defaultValue: Boolean): Boolean =
        settingsStorage.decodeBool(key, defaultValue)

    fun decodeSettingsStringSet(key: String): Set<String>? =
        settingsStorage.decodeStringSet(key)?.toSet()

    fun encodeStartOnBoot(startOnBoot: Boolean) {
        encodeSettings(PREF_IS_BOOTED, startOnBoot)
    }

    fun decodeStartOnBoot(): Boolean = decodeSettingsBool(PREF_IS_BOOTED, false)

    fun encodeWebDavConfig(config: WebDavConfig): Boolean =
        mainStorage.encode(KEY_WEBDAV_CONFIG, JsonUtil.toJson(config))

    fun decodeWebDavConfig(): WebDavConfig? {
        val json = mainStorage.decodeString(KEY_WEBDAV_CONFIG) ?: return null
        return JsonUtil.fromJson(json, WebDavConfig::class.java)
    }

    fun setVpnSessionActive(active: Boolean) {
        mainStorage.encode(AppConfig.PREF_VPN_SESSION_ACTIVE, active)
    }

    fun isVpnSessionActive(): Boolean =
        mainStorage.decodeBool(AppConfig.PREF_VPN_SESSION_ACTIVE, false)

    fun setVpnSessionStartEpochMs(epochMs: Long) {
        mainStorage.encode(AppConfig.PREF_VPN_SESSION_START_EPOCH_MS, epochMs)
    }

    fun getVpnSessionStartEpochMs(): Long =
        mainStorage.decodeLong(AppConfig.PREF_VPN_SESSION_START_EPOCH_MS, 0L)

    fun setVpnSessionGuid(guid: String?) {
        if (guid.isNullOrBlank()) {
            mainStorage.remove(AppConfig.PREF_VPN_SESSION_GUID)
        } else {
            mainStorage.encode(AppConfig.PREF_VPN_SESSION_GUID, guid)
        }
    }

    fun getVpnSessionGuid(): String? {
        val g = mainStorage.decodeString(AppConfig.PREF_VPN_SESSION_GUID)
        return g?.takeIf { it.isNotBlank() }
    }

    fun clearVpnSessionRuntime() {
        setVpnSessionActive(false)
        setVpnSessionStartEpochMs(0L)
        setVpnSessionGuid(null)
    }

    fun setVpnSessionLastAutoSaveStartMs(epochMs: Long) {
        mainStorage.encode(AppConfig.PREF_VPN_SESSION_LAST_AUTO_SAVE_START_MS, epochMs)
    }

    fun getVpnSessionLastAutoSaveStartMs(): Long =
        mainStorage.decodeLong(AppConfig.PREF_VPN_SESSION_LAST_AUTO_SAVE_START_MS, 0L)

    /** Key for storing the last auto-save notification timestamp to prevent duplicate notifications. */
    const val KEY_LAST_AUTO_SAVE_NOTIFICATION_TIMESTAMP = "last_auto_save_notification_timestamp"

    /** Key for storing JSON array of profile summaries (guid + remarks) saved during auto-save. */
    const val KEY_LAST_AUTO_SAVE_PROFILES_JSON = "last_auto_save_profiles_json"

    fun setLastAutoSaveNotificationTimestamp(epochMs: Long) {
        mainStorage.encode(KEY_LAST_AUTO_SAVE_NOTIFICATION_TIMESTAMP, epochMs)
    }

    fun getLastAutoSaveNotificationTimestamp(): Long =
        mainStorage.decodeLong(KEY_LAST_AUTO_SAVE_NOTIFICATION_TIMESTAMP, 0L)

    fun setLastAutoSaveProfilesJson(profilesJson: String) {
        mainStorage.encode(KEY_LAST_AUTO_SAVE_PROFILES_JSON, profilesJson)
    }

    fun getLastAutoSaveProfilesJson(): String? =
        mainStorage.decodeString(KEY_LAST_AUTO_SAVE_PROFILES_JSON)

    fun getThemeMode(): String? = decodeSettingsString(KEY_THEME_MODE)

    fun setThemeMode(mode: String): Boolean {
        return encodeSettings(KEY_THEME_MODE, mode)
    }

    fun isAutosaveEnabled(): Boolean = decodeSettingsBool(KEY_AUTOSAVE_ENABLED, true)

    fun setAutosaveMode(enabled: Boolean): Boolean {
        return encodeSettings(KEY_AUTOSAVE_ENABLED, enabled)
    }
}

private const val KEY_AUTOSAVE_ENABLED = "autosave_enabled"
private const val KEY_THEME_MODE = "theme_mode"
