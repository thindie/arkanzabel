package com.v2ray.ang.dto

/**
 * Built JSON document for the core (Xray), ready for [libv2ray.CoreController.startLoop].
 */
data class V2Ray(
  val guid: String,
  val json: String,
)
