package com.thindie.rknzbl.feature.settings.ui

import com.thindie.rknzbl.R
import com.v2ray.ang.AppConfig

/**
 * Which ports Sniffing should restrict its attention to.
 *
 *  - [All] sniff every port (no restriction).
 *  - [Common] sniff only common web ports (80, 443).
 *  - [Http] sniff only HTTP ports (80, 8080, 8880).
 *  - [Https] sniff only HTTPS ports (443, 8443).
 *
 * The enum carries no state of its own — the storage key and the UI label are resolved
 * through the extension functions below, following the theme-mapper convention.
 */
enum class SniffingPortRange {
  All,
  Common,
  Http,
  Https,
}

/** Storage key for this [SniffingPortRange] value in KeyValueStorage. */
fun SniffingPortRange.toStorageString(): String =
  when (this) {
    SniffingPortRange.All -> AppConfig.SNIFFING_PORT_RANGE_ALL
    SniffingPortRange.Common -> AppConfig.SNIFFING_PORT_RANGE_COMMON
    SniffingPortRange.Http -> AppConfig.SNIFFING_PORT_RANGE_HTTP
    SniffingPortRange.Https -> AppConfig.SNIFFING_PORT_RANGE_HTTPS
  }

/** Convert a stored string to [SniffingPortRange], defaulting to [SniffingPortRange.All]. */
fun toSniffingPortRange(string: String): SniffingPortRange =
  SniffingPortRange.entries.firstOrNull { it.toStorageString() == string } ?: SniffingPortRange.All

/** String resource id for the UI label of this [SniffingPortRange] value. */
val SniffingPortRange.labelRes: Int
  get() =
    when (this) {
      SniffingPortRange.All -> R.string.settings_sniffing_port_range_all
      SniffingPortRange.Common -> R.string.settings_sniffing_port_range_common
      SniffingPortRange.Http -> R.string.settings_sniffing_port_range_http
      SniffingPortRange.Https -> R.string.settings_sniffing_port_range_https
    }
