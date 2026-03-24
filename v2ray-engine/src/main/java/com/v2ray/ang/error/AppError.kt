package com.v2ray.ang.error

sealed class AppError(
  message: String,
  cause: Throwable? = null,
) : RuntimeException(message, cause)

class IncomingConfigError(
  message: String,
  val source: String? = null,
  cause: Throwable? = null,
) : AppError(message, cause)

class OutboundConfigError(
  message: String,
  val source: String? = null,
  cause: Throwable? = null,
) : AppError(message, cause)

class RoutingConfigError(
  message: String,
  val source: String? = null,
  cause: Throwable? = null,
) : AppError(message, cause)

class DnsConfigError(
  message: String,
  val source: String? = null,
  cause: Throwable? = null,
) : AppError(message, cause)

class ConfigBuildError(
  message: String,
  val stage: String,
  cause: Throwable? = null,
) : AppError(message, cause)
