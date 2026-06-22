package com.v2ray.ang.dto

data class SubscriptionItem(
  val remarks: String,
  val url: String,
  val enabled: Boolean = true,
  val addedTime: Long = System.currentTimeMillis(),
  val lastUpdated: Long = -1,
  val autoUpdate: Boolean = false,
  val updateInterval: Int? = null,
  val prevProfile: String? = null,
  val nextProfile: String? = null,
  val filter: String? = null,
  val allowInsecureUrl: Boolean = false,
  val userAgent: String? = null,
)
