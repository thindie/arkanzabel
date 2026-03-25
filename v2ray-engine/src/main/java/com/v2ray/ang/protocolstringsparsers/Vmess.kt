package com.v2ray.ang.protocolstringsparsers

import android.text.TextUtils
import android.util.Log
import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.ConnectionProfile
import com.v2ray.ang.dto.V2rayConfig.Outbound
import com.v2ray.ang.dto.VmessQRCode
import com.v2ray.ang.enums.Protocol
import com.v2ray.ang.enums.NetworkType
import com.v2ray.ang.extension.idnHost
import com.v2ray.ang.extension.nullIfBlank
import com.v2ray.ang.runtime.KeyValueStorage
import com.v2ray.ang.runtime.V2rayConfigManager
import com.v2ray.ang.util.JsonUtil
import com.v2ray.ang.util.Utils
import java.net.URI

object Vmess : ProtocolParser() {
  fun parse(str: String): ConnectionProfile? {
    if (str.indexOf('?') > 0 && str.indexOf('&') > 0) {
      return parseVmessStd(str)
    }

    val allowInsecure = KeyValueStorage.decodeSettingsBool(AppConfig.PREF_ALLOW_INSECURE, false)
    val config = ConnectionProfile.create(Protocol.Vmess)

    var result = str.replace(Protocol.Vmess.protocolScheme, "")
    result = Utils.decode(result)
    if (TextUtils.isEmpty(result)) {
      Log.w(AppConfig.TAG, "Toast decoding failed")
      return null
    }
    val vmessQRCode = JsonUtil.fromJson(result, VmessQRCode::class.java) ?: return null
    // Although VmessQRCode fields are non null, looks like Gson may still create null fields
    if (TextUtils.isEmpty(vmessQRCode.add)
      || TextUtils.isEmpty(vmessQRCode.port)
      || TextUtils.isEmpty(vmessQRCode.id)
      || TextUtils.isEmpty(vmessQRCode.net)
    ) {
      Log.w(AppConfig.TAG, "Toast incorrect protocol")
      return null
    }

    config.remarks = vmessQRCode.ps
    config.server = vmessQRCode.add
    config.serverPort = vmessQRCode.port
    config.password = vmessQRCode.id
    config.method =
      if (TextUtils.isEmpty(vmessQRCode.scy)) AppConfig.DEFAULT_SECURITY else vmessQRCode.scy

    config.network = vmessQRCode.net
    if (config.network.isNullOrEmpty()) {
      config.network = NetworkType.TCP.type
    }
    config.headerType = vmessQRCode.type
    config.host = vmessQRCode.host
    config.path = vmessQRCode.path

    when (NetworkType.fromString(config.network)) {
      NetworkType.KCP -> {
        config.seed = vmessQRCode.path
      }

      NetworkType.GRPC -> {
        config.mode = vmessQRCode.type
        config.serviceName = vmessQRCode.path
        config.authority = vmessQRCode.host
      }

      else -> {}
    }

    config.security = vmessQRCode.tls
    config.sni = vmessQRCode.sni
    config.fingerPrint = vmessQRCode.fp
    config.alpn = vmessQRCode.alpn
    config.insecure = when (vmessQRCode.insecure) {
      "1" -> true
      "0" -> false
      else -> allowInsecure
    }
    return config
  }

  fun toUri(config: ConnectionProfile): String {
    val vmessQRCode = VmessQRCode()

    vmessQRCode.v = "2"
    vmessQRCode.ps = config.remarks
    vmessQRCode.add = config.server.orEmpty()
    vmessQRCode.port = config.serverPort.orEmpty()
    vmessQRCode.id = config.password.orEmpty()
    vmessQRCode.scy = config.method.orEmpty()
    vmessQRCode.aid = "0"

    vmessQRCode.net = config.network.orEmpty()
    vmessQRCode.type = config.headerType.orEmpty()
    when (NetworkType.fromString(config.network)) {
      NetworkType.KCP -> {
        vmessQRCode.path = config.seed.orEmpty()
      }

      NetworkType.GRPC -> {
        vmessQRCode.type = config.mode.orEmpty()
        vmessQRCode.path = config.serviceName.orEmpty()
        vmessQRCode.host = config.authority.orEmpty()
      }

      else -> {}
    }

    config.host?.nullIfBlank()?.let { vmessQRCode.host = it }
    config.path?.nullIfBlank()?.let { vmessQRCode.path = it }

    vmessQRCode.tls = config.security.orEmpty()
    vmessQRCode.sni = config.sni.orEmpty()
    vmessQRCode.fp = config.fingerPrint.orEmpty()
    vmessQRCode.alpn = config.alpn.orEmpty()
    vmessQRCode.insecure = if (config.insecure) "1" else "0"

    val json = JsonUtil.toJson(vmessQRCode)
    return Utils.encode(json)
  }

  fun parseVmessStd(str: String): ConnectionProfile? {
    val allowInsecure = KeyValueStorage.decodeSettingsBool(AppConfig.PREF_ALLOW_INSECURE, false)
    val config = ConnectionProfile.create(Protocol.Vmess)

    val uri = URI(Utils.fixIllegalUrl(str))
    if (uri.rawQuery.isNullOrEmpty()) return null
    val queryParam = getQueryParam(uri)

    config.remarks = Utils.decodeURIComponent(uri.fragment.orEmpty()).let { it.ifEmpty { "none" } }
    config.server = uri.idnHost
    config.serverPort = uri.port.toString()
    config.password = uri.userInfo
    config.method = AppConfig.DEFAULT_SECURITY

    getItemFormQuery(config, queryParam, allowInsecure)

    return config
  }

  fun toOutbound(connectionProfile: ConnectionProfile): Outbound? {
    val outbound = V2rayConfigManager.createInitOutbound(Protocol.Vmess)

    outbound?.settings?.vnext?.first()?.let { vnext ->
      vnext.address = getServerAddress(connectionProfile)
      vnext.port = connectionProfile.serverPort.orEmpty().toInt()
      vnext.users[0].id = connectionProfile.password.orEmpty()
      vnext.users[0].security = connectionProfile.method
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