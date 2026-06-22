package com.v2ray.ang.protocolstringsparsers

import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.ConnectionProfile
import com.v2ray.ang.enums.NetworkType
import com.v2ray.ang.extension.nullIfBlank
import com.v2ray.ang.runtime.KeyValueStorage
import com.v2ray.ang.util.HttpUtil
import com.v2ray.ang.util.Utils
import java.net.URI

open class ProtocolParser {
  private fun firstQueryValueIgnoreCase(
    queryParam: Map<String, String>,
    vararg names: String,
  ): String? {
    for (name in names) {
      val v =
        queryParam.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value?.trim()
      if (!v.isNullOrEmpty()) return v
    }
    return null
  }

  /**
   * Converts a ProfileItem object to a URI string.
   *
   * @param config the ProfileItem object to convert
   * @param userInfo the user information to include in the URI
   * @param dicQuery the query parameters to include in the URI
   * @return the converted URI string
   */
  fun toUri(
    config: ConnectionProfile,
    userInfo: String?,
    dicQuery: HashMap<String, String>?,
  ): String {
    val query =
      if (dicQuery != null) {
        "?" +
          dicQuery.toList().joinToString(
            separator = "&",
            transform = { it.first + "=" + Utils.encodeURIComponent(it.second) },
          )
      } else {
        ""
      }

    val url =
      String.format(
        "%s@%s:%s",
        Utils.encodeURIComponent(userInfo ?: ""),
        Utils.getIpv6Address(HttpUtil.toIdnDomain(config.server.orEmpty())),
        config.serverPort,
      )

    return "${url}$query#${Utils.encodeURIComponent(config.remarks)}"
  }

  /**
   * Extracts query parameters from a URI.
   *
   * @param uri the URI to extract query parameters from
   * @return a map of query parameters
   */
  fun getQueryParam(uri: URI): Map<String, String> {
    val raw = uri.rawQuery ?: return emptyMap()
    return raw.split("&")
      .mapNotNull { segment ->
        if (segment.isEmpty()) return@mapNotNull null
        val eq = segment.indexOf('=')
        if (eq < 0) {
          segment to ""
        } else {
          val k = segment.substring(0, eq)
          val v = segment.substring(eq + 1)
          k to Utils.decodeURIComponent(v)
        }
      }
      .toMap()
  }

  /**
   * Returns a copy of [config] with transport/TLS fields taken from URI query parameters.
   *
   * @param config base profile (e.g. host/port/user already set from the URI)
   * @param queryParam parsed query map
   * @param allowInsecure default when query omits insecure flags
   */
  fun getItemFormQuery(
    config: ConnectionProfile,
    queryParam: Map<String, String>,
    allowInsecure: Boolean,
  ): ConnectionProfile {
    val allowInsecureKeys = arrayOf("insecure", "allowInsecure", "allow_insecure")
    val insecureResolved =
      when {
        allowInsecureKeys.any { queryParam[it] == "1" } -> true
        allowInsecureKeys.any { queryParam[it] == "0" } -> false
        else -> allowInsecure
      }
    return config.copy(
      network = queryParam["type"] ?: NetworkType.TCP.type,
      headerType = queryParam["headerType"],
      host = queryParam["host"],
      path = queryParam["path"],
      seed = queryParam["seed"],
      quicSecurity = queryParam["quicSecurity"],
      quicKey = queryParam["key"],
      mode = queryParam["mode"],
      serviceName = queryParam["serviceName"],
      authority = queryParam["authority"],
      xhttpMode = queryParam["mode"],
      xhttpExtra = queryParam["extra"],
      security =
        queryParam["security"]?.trim()?.lowercase().takeIf {
          it == AppConfig.TLS || it == AppConfig.REALITY
        },
      insecure = insecureResolved,
      sni = queryParam["sni"],
      fingerPrint = queryParam["fp"],
      alpn = queryParam["alpn"],
      echConfigList = queryParam["ech"],
      pinnedCA256 = queryParam["pcs"],
      publicKey =
        firstQueryValueIgnoreCase(
          queryParam,
          "pbk",
          "publicKey",
          "public_key",
          "public-key",
        ),
      shortId = queryParam["sid"],
      spiderX = queryParam["spx"],
      mldsa65Verify = queryParam["pqv"],
      flow = queryParam["flow"],
    )
  }

  /**
   * Creates a map of query parameters from a ProfileItem object.
   *
   * @param config the ProfileItem object to create query parameters from
   * @return a map of query parameters
   */
  fun getQueryDic(config: ConnectionProfile): HashMap<String, String> {
    val dicQuery = HashMap<String, String>()
    dicQuery["security"] = config.security?.ifEmpty { "none" }.orEmpty()
    config.sni?.nullIfBlank()?.let { dicQuery["sni"] = it }
    config.alpn?.nullIfBlank()?.let { dicQuery["alpn"] = it }
    config.echConfigList?.nullIfBlank()?.let { dicQuery["ech"] = it }
    config.pinnedCA256?.nullIfBlank()?.let { dicQuery["pcs"] = it }
    config.fingerPrint?.nullIfBlank()?.let { dicQuery["fp"] = it }
    config.publicKey?.nullIfBlank()?.let { dicQuery["pbk"] = it }
    config.shortId?.nullIfBlank()?.let { dicQuery["sid"] = it }
    config.spiderX?.nullIfBlank()?.let { dicQuery["spx"] = it }
    config.mldsa65Verify?.nullIfBlank()?.let { dicQuery["pqv"] = it }
    config.flow?.nullIfBlank()?.let { dicQuery["flow"] = it }
    // Add two keys for compatibility: "insecure" and "allowInsecure"
    if (config.security == AppConfig.TLS) {
      val insecureFlag = if (config.insecure) "1" else "0"
      dicQuery["insecure"] = insecureFlag
      dicQuery["allowInsecure"] = insecureFlag
    }

    val networkType = NetworkType.fromString(config.network)
    dicQuery["type"] = networkType.type

    when (networkType) {
      NetworkType.TCP -> {
        dicQuery["headerType"] = config.headerType?.ifEmpty { "none" }.orEmpty()
        config.host?.nullIfBlank()?.let { dicQuery["host"] = it }
      }

      NetworkType.KCP -> {
        dicQuery["headerType"] = config.headerType?.ifEmpty { "none" }.orEmpty()
        config.seed?.nullIfBlank()?.let { dicQuery["seed"] = it }
      }

      NetworkType.WS, NetworkType.HTTP_UPGRADE -> {
        config.host?.nullIfBlank()?.let { dicQuery["host"] = it }
        config.path?.nullIfBlank()?.let { dicQuery["path"] = it }
      }

      NetworkType.XHTTP -> {
        config.host?.nullIfBlank()?.let { dicQuery["host"] = it }
        config.path?.nullIfBlank()?.let { dicQuery["path"] = it }
        config.xhttpMode?.nullIfBlank()?.let { dicQuery["mode"] = it }
        config.xhttpExtra?.nullIfBlank()?.let { dicQuery["extra"] = it }
      }

      NetworkType.HTTP, NetworkType.H2 -> {
        dicQuery["type"] = "http"
        config.host?.nullIfBlank()?.let { dicQuery["host"] = it }
        config.path?.nullIfBlank()?.let { dicQuery["path"] = it }
      }

      NetworkType.GRPC -> {
        config.mode?.nullIfBlank()?.let { dicQuery["mode"] = it }
        config.authority?.nullIfBlank()?.let { dicQuery["authority"] = it }
        config.serviceName?.nullIfBlank()?.let { dicQuery["serviceName"] = it }
      }

      else -> {}
    }

    return dicQuery
  }

  fun getServerAddress(connectionProfile: ConnectionProfile): String {
    if (Utils.isPureIpAddress(connectionProfile.server.orEmpty())) {
      return connectionProfile.server.orEmpty()
    }

    val domain = HttpUtil.toIdnDomain(connectionProfile.server.orEmpty())
    if (KeyValueStorage.decodeSettingsString(
        AppConfig.PREF_OUTBOUND_DOMAIN_RESOLVE_METHOD,
        "1",
      ) != "2"
    ) {
      return domain
    }
    // Resolve and replace domain
    val resolvedIps =
      HttpUtil.resolveHostToIP(
        domain,
        KeyValueStorage.decodeSettingsBool(AppConfig.PREF_PREFER_IPV6),
      )
    if (resolvedIps.isNullOrEmpty()) {
      return domain
    }
    return resolvedIps.first()
  }
}
