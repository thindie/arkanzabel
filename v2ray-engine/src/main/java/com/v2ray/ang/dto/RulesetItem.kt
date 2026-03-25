package com.v2ray.ang.dto

data class RulesetItem(
    val remarks: String? =null,
    val ip: List<String>? = null,
    val domain: List<String>? = null,
    val outboundTag: String,
    val port: String? = null,
    val network: String? = null,
    val protocol: List<String>? = null,
    val enabled: Boolean = true,
    val locked: Boolean = false,
)