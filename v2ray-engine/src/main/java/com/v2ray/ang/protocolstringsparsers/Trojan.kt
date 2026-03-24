package com.v2ray.ang.protocolstringsparsers

import com.v2ray.ang.AppConfig
import com.v2ray.ang.enums.Protocol
import com.v2ray.ang.enums.NetworkType
import com.v2ray.ang.dto.ConnectionProfile
import com.v2ray.ang.dto.V2rayConfig.OutboundBean
import com.v2ray.ang.extension.idnHost
import com.v2ray.ang.runtime.KeyValueStorage
import com.v2ray.ang.runtime.V2rayConfigManager
import com.v2ray.ang.util.Utils
import java.net.URI

object Trojan : ProtocolParser() {
    /**
     * Parses a Trojan URI string into a ProfileItem object.
     *
     * @param str the Trojan URI string to parse
     * @return the parsed ProfileItem object, or null if parsing fails
     */
    fun parse(str: String): ConnectionProfile? {
        var allowInsecure = KeyValueStorage.decodeSettingsBool(AppConfig.PREF_ALLOW_INSECURE, false)
        val config = ConnectionProfile.create(Protocol.Trojan)

        val uri = URI(Utils.fixIllegalUrl(str))
        config.remarks = Utils.decodeURIComponent(uri.fragment.orEmpty()).let { it.ifEmpty { "none" } }
        config.server = uri.idnHost
        config.serverPort = uri.port.toString()
        config.password = uri.userInfo

        if (uri.rawQuery.isNullOrEmpty()) {
            config.network = NetworkType.TCP.type
            config.security = AppConfig.TLS
            config.insecure = allowInsecure
        } else {
            val queryParam = getQueryParam(uri)

            getItemFormQuery(config, queryParam, allowInsecure)
            config.security = queryParam["security"] ?: AppConfig.TLS
        }

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

        return toUri(config, config.password, dicQuery)
    }

    /**
     * Converts a ProfileItem object to an OutboundBean object.
     *
     * @param connectionProfile the ProfileItem object to convert
     * @return the converted OutboundBean object, or null if conversion fails
     */
    fun toOutbound(connectionProfile: ConnectionProfile): OutboundBean? {
        val outboundBean = V2rayConfigManager.createInitOutbound(Protocol.Trojan)

        outboundBean?.settings?.servers?.first()?.let { server ->
            server.address = getServerAddress(connectionProfile)
            server.port = connectionProfile.serverPort.orEmpty().toInt()
            server.password = connectionProfile.password
            server.flow = connectionProfile.flow
        }

        val sni = outboundBean?.streamSettings?.let {
            V2rayConfigManager.populateTransportSettings(it, connectionProfile)
        }

        outboundBean?.streamSettings?.let {
            V2rayConfigManager.populateTlsSettings(it, connectionProfile, sni)
        }

        return outboundBean
    }
}