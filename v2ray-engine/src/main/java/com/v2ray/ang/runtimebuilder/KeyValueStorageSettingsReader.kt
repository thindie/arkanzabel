package com.v2ray.ang.runtimebuilder

import com.v2ray.ang.runtime.KeyValueStorage
import com.v2ray.ang.runtime.SettingsManager

/**
 * Reads settings from the global [KeyValueStorage] singleton.
 * Bridge between the new [SettingsReader] interface and existing static storage.
 */
class KeyValueStorageSettingsReader : SettingsReader {
  override fun getBool(
    key: String,
    defaultValue: Boolean,
  ): Boolean = KeyValueStorage.decodeSettingsBool(key, defaultValue)

  override fun getString(key: String): String? = KeyValueStorage.decodeSettingsString(key)

  override fun getString(
    key: String,
    defaultValue: String?,
  ): String? = KeyValueStorage.decodeSettingsString(key, defaultValue)

  override fun getInt(
    key: String,
    defaultValue: Int,
  ): Int = KeyValueStorage.decodeSettingsStringAsInt(key, defaultValue)

  override fun getRemoteDnsServers(): List<String> = SettingsManager.getRemoteDnsServers()

  override fun getDomesticDnsServers(): List<String> = SettingsManager.getDomesticDnsServers()

  override fun isVpnMode(): Boolean = SettingsManager.isVpnMode()

  override fun isUsingHevTun(): Boolean = SettingsManager.isUsingHevTun()
}
