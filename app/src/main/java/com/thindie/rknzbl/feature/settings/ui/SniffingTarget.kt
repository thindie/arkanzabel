package com.thindie.rknzbl.feature.settings.ui

import com.thindie.rknzbl.R
import com.v2ray.ang.AppConfig

/**
 * Which protocols the Sniffing should inspect.
 *
 *  - [All] inspects every supported protocol (http, tls, quic, h2).
 *  - [Http] inspects only HTTP traffic.
 *  - [Tls] inspects only TLS traffic.
 *  - [Quic] inspects only QUIC traffic.
 *
 * The enum carries no state of its own — the storage key and the UI label are resolved
 * through the extension functions below, following the theme-mapper convention.
 */
enum class SniffingTarget {
  All,
  Http,
  Tls,
  Quic,
}

/** Storage key for this [SniffingTarget] value in KeyValueStorage. */
fun SniffingTarget.toStorageString(): String =
  when (this) {
    SniffingTarget.All -> AppConfig.SNIFFING_TARGET_ALL
    SniffingTarget.Http -> AppConfig.SNIFFING_TARGET_HTTP
    SniffingTarget.Tls -> AppConfig.SNIFFING_TARGET_TLS
    SniffingTarget.Quic -> AppConfig.SNIFFING_TARGET_QUIC
  }

/** Convert a stored string to [SniffingTarget], defaulting to [SniffingTarget.All]. */
fun toSniffingTarget(string: String): SniffingTarget =
  SniffingTarget.entries.firstOrNull { it.toStorageString() == string } ?: SniffingTarget.All

/** String resource id for the UI label of this [SniffingTarget] value. */
val SniffingTarget.labelRes: Int
  get() =
    when (this) {
      SniffingTarget.All -> R.string.settings_sniffing_target_all
      SniffingTarget.Http -> R.string.settings_sniffing_target_http
      SniffingTarget.Tls -> R.string.settings_sniffing_target_tls
      SniffingTarget.Quic -> R.string.settings_sniffing_target_quic
    }
