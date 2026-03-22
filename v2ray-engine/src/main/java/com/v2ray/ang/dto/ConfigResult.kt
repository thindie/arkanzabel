package com.v2ray.ang.dto

data class ConfigResult(
    val status: Boolean,
    val guid: String? = null,
    val content: String = "",
)
