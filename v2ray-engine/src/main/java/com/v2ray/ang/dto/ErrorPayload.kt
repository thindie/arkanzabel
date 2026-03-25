package com.v2ray.ang.dto

/**
 * Machine-oriented context for logs, support, and future UI mapping (not shown to users as-is).
 */
data class ErrorPayload(
  val stage: String? = null,
  val source: String? = null,
  val extras: Map<String, String> = emptyMap(),
)
