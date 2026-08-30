package com.v2ray.ang.runtimebuilder

/**
 * Abstraction over settings storage for runtimebuilder classes.
 * Extracted from static [com.v2ray.ang.runtime.KeyValueStorage] to enable unit testing.
 */
interface SettingsReader {
  fun getBool(
    key: String,
    defaultValue: Boolean,
  ): Boolean

  fun getString(key: String): String?

  fun getString(
    key: String,
    defaultValue: String?,
  ): String?

  fun getInt(
    key: String,
    defaultValue: Int,
  ): Int

  fun getRemoteDnsServers(): List<String>

  fun getDomesticDnsServers(): List<String>

  fun isVpnMode(): Boolean

  fun isUsingHevTun(): Boolean
}
