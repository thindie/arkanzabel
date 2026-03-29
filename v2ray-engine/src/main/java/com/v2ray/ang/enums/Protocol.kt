package com.v2ray.ang.enums

import com.v2ray.ang.AppConfig

enum class Protocol(val value: Int, val protocolScheme: String) {
    Vmess(1, AppConfig.VMESS),
    Custom(2, AppConfig.CUSTOM),
    ShadowSocks(3, AppConfig.SHADOWSOCKS),
    Socks(4, AppConfig.SOCKS),
    Vless(5, AppConfig.VLESS),
    Trojan(6, AppConfig.TROJAN),
    WireGuard(7, AppConfig.WIREGUARD),
    Hysteria2(9, AppConfig.HYSTERIA2),
    Hysteria(900, AppConfig.HYSTERIA),
    Http(10, AppConfig.HTTP),
    PolicyGroup (101, AppConfig.CUSTOM);

    companion object {
        fun fromInt(value: Int) = entries.firstOrNull { it.value == value }
    }
}