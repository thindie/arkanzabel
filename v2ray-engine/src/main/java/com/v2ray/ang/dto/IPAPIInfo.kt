package com.v2ray.ang.dto

import com.google.gson.annotations.SerializedName

/**
 * JSON from different IP / geo HTTP APIs (keys differ by provider).
 * [com.v2ray.ang.runtime.SpeedtestManager.getRemoteIPInfo] picks the first non-blank candidate in a fixed order.
 */
data class IPAPIInfo(
  val ip: String? = null,
  @SerializedName("client_ip") val clientIp: String? = null,
  @SerializedName("ip_addr") val ipAddr: String? = null,
  val query: String? = null,
  val country: String? = null,
  @SerializedName("country_name") val countryName: String? = null,
  @SerializedName("country_code") val countryCodeSnake: String? = null,
  val countryCode: String? = null,
  val location: Location? = null,
) {
  data class Location(
    @SerializedName("country_code") val countryCode: String? = null,
  )
}
