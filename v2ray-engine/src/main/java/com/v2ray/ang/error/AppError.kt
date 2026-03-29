package com.v2ray.ang.error

import com.v2ray.ang.dto.ErrorPayload

sealed class AppError(
  message: String,
  cause: Throwable? = null,
  val userReadable: String = message,
  val payload: ErrorPayload? = null,
) : RuntimeException(message, cause)

class IncomingConfigError(
  message: String,
  val source: String? = null,
  cause: Throwable? = null,
  userReadable: String = message,
  explicitPayload: ErrorPayload? = null,
) : AppError(
  message,
  cause,
  userReadable,
  explicitPayload ?: source?.let { ErrorPayload(source = it) },
)

class OutboundConfigError(
  message: String,
  val source: String? = null,
  cause: Throwable? = null,
  userReadable: String = message,
  explicitPayload: ErrorPayload? = null,
) : AppError(
  message,
  cause,
  userReadable,
  explicitPayload ?: source?.let { ErrorPayload(source = it) },
)

class RoutingConfigError(
  message: String,
  val source: String? = null,
  cause: Throwable? = null,
  userReadable: String = message,
  explicitPayload: ErrorPayload? = null,
) : AppError(
  message,
  cause,
  userReadable,
  explicitPayload ?: source?.let { ErrorPayload(source = it) },
)

class DnsConfigError(
  message: String,
  val source: String? = null,
  cause: Throwable? = null,
  userReadable: String = message,
  explicitPayload: ErrorPayload? = null,
) : AppError(
  message,
  cause,
  userReadable,
  explicitPayload ?: source?.let { ErrorPayload(source = it) },
)

class ConfigBuildError(
  message: String,
  val stage: String,
  cause: Throwable? = null,
  userReadable: String = message,
) : AppError(
  message,
  cause,
  userReadable,
  ErrorPayload(stage = stage),
)

class ProfileNotFoundError(
  val guid: String,
) : AppError(
  message = "No profile for guid=$guid",
  userReadable = "Selected profile was not found",
  payload = ErrorPayload(stage = "profile", extras = mapOf("guid" to guid)),
)

class StoredRawMissingError(
  val guid: String,
) : AppError(
  message = "No stored raw JSON for custom profile guid=$guid",
  userReadable = "Custom profile data is missing",
  payload = ErrorPayload(stage = "customRaw", extras = mapOf("guid" to guid)),
)

class ConfigValidationError(
  message: String,
  userReadable: String = message,
  stage: String = "validation",
  extras: Map<String, String> = emptyMap(),
) : AppError(
  message,
  null,
  userReadable,
  ErrorPayload(stage = stage, extras = extras),
)

class AssetConfigMissingError(
  val templateName: String,
) : AppError(
  message = "Empty or unreadable template: $templateName",
  userReadable = "Built-in VPN template is missing",
  payload = ErrorPayload(stage = "assetTemplate", extras = mapOf("template" to templateName)),
)

class ConfigSerializationError(
  val stage: String,
) : AppError(
  message = "Failed to serialize config at stage=$stage",
  userReadable = "Could not build configuration JSON",
  payload = ErrorPayload(stage = stage),
)
