package com.v2ray.ang.enums

/** TLS-layer security used by an outbound's stream settings. Anything else means no TLS. */
enum class Security(val value: String) {
  TLS("tls"),
  REALITY("reality"),
  ;

  companion object {
    fun fromString(value: String?): Security? = entries.find { it.value == value?.trim()?.lowercase() }
  }
}
