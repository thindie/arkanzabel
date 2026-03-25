package com.v2ray.ang.protocolstringsparsers

import com.v2ray.ang.AppConfig
import com.v2ray.ang.enums.Protocol
import com.v2ray.ang.dto.ConnectionProfile
import com.v2ray.ang.dto.V2rayConfig.Outbound
import com.v2ray.ang.extension.idnHost
import com.v2ray.ang.runtime.KeyValueStorage
import com.v2ray.ang.runtime.V2rayConfigManager
import com.v2ray.ang.util.Utils
import java.net.URI

object Vless : ProtocolParser() {

    private val realityPublicKeyFromRawRegexes =
      listOf(
        Regex("""(?i)(?:^|[?&])pbk=([^&#]+)"""),
        Regex("""(?i)(?:^|[?&])publicKey=([^&#]+)"""),
        Regex("""(?i)(?:^|[?&])public_key=([^&#]+)"""),
        Regex("""(?i)(?:^|[?&])public-key=([^&#]+)"""),
      )

    /**
     * Some exporters or [URI] parsing edge cases drop query pairs; scrape pbk from the raw share line.
     */
    private fun mergeRealityPublicKeyFromRawLine(config: ConnectionProfile, rawLine: String) {
      if (!config.publicKey.isNullOrBlank()) return
      val chunks = buildList {
        rawLine.substringAfter("?", "").substringBefore("#").trim().let { if (it.isNotEmpty()) add(it) }
        rawLine.substringAfter("#", "").trim().let {
          if (it.contains('=')) add(it)
        }
      }
      for (chunk in chunks) {
        for (pattern in realityPublicKeyFromRawRegexes) {
          val match = pattern.find(chunk) ?: continue
          val encoded = match.groupValues[1].trim()
          if (encoded.isEmpty()) continue
          val decoded = Utils.decodeURIComponent(encoded)
          if (decoded.isNotBlank()) {
            config.publicKey = decoded
            return
          }
        }
      }
    }

    /**
     * Parses a Vless URI string into a ProfileItem object.
     *
     * @param str the Vless URI string to parse
     * @return the parsed ProfileItem object, or null if parsing fails
     */
    fun parse(str: String): ConnectionProfile? {
        var allowInsecure = KeyValueStorage.decodeSettingsBool(AppConfig.PREF_ALLOW_INSECURE, false)
        val config = ConnectionProfile.create(Protocol.Vless)

        val uri = URI(Utils.fixIllegalUrl(str))
        if (uri.rawQuery.isNullOrEmpty()) return null
        val queryParam = getQueryParam(uri)

        config.remarks = Utils.decodeURIComponent(uri.fragment.orEmpty()).let { it.ifEmpty { "none" } }
        config.server = uri.idnHost
        config.serverPort = uri.port.toString()
        config.password = uri.userInfo
        config.method = queryParam["encryption"] ?: "none"

        getItemFormQuery(config, queryParam, allowInsecure)
        mergeRealityPublicKeyFromRawLine(config, str)

        return config
    }

    /**
     * Converts a ProfileItem object to a URI string.
     *
     * @param config the ProfileItem object to convert
     * @return the converted URI string
     */
    fun toUri(config: ConnectionProfile): String {
        val dicQuery = getQueryDic(config)
        dicQuery["encryption"] = config.method ?: "none"

        return toUri(config, config.password, dicQuery)
    }

    /**
     * Converts a ProfileItem object to an Outbound object.
     *
     * @param connectionProfile the ProfileItem object to convert
     * @return the converted Outbound object, or null if conversion fails
     */
    fun toOutbound(connectionProfile: ConnectionProfile): Outbound? {
        if (connectionProfile.security == AppConfig.REALITY && connectionProfile.publicKey.isNullOrBlank()) {
            return null
        }
        val outbound = V2rayConfigManager.createInitOutbound(Protocol.Vless)

        outbound?.settings?.vnext?.first()?.let { vnext ->
            vnext.address = getServerAddress(connectionProfile)
            vnext.port = connectionProfile.serverPort.orEmpty().toInt()
            vnext.users[0].id = connectionProfile.password.orEmpty()
            vnext.users[0].encryption = connectionProfile.method
            vnext.users[0].flow = connectionProfile.flow
        }

        val sni = outbound?.streamSettings?.let {
            V2rayConfigManager.populateTransportSettings(it, connectionProfile)
        }

        outbound?.streamSettings?.let {
            V2rayConfigManager.populateTlsSettings(it, connectionProfile, sni)
        }

        return outbound
    }
}