package com.v2ray.ang.dto

data class AssetUrlItem(
    val remarks: String,
    val url: String,
    val addedTime: Long = System.currentTimeMillis(),
    val lastUpdated: Long = -1,
    val locked: Boolean = false,
)