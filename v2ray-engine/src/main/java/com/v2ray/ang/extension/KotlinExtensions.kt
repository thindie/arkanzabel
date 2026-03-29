package com.v2ray.ang.extension

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.core.content.IntentCompat
import androidx.core.os.BundleCompat
import java.io.Serializable
import java.net.URI
import java.util.Locale

fun Long.toSpeedString(): String = toTrafficString() + "/s"

private const val TRAFFIC_UNIT_STEP = 1024.0

fun Long.toTrafficString(): String {
    val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB")
    var size = toDouble()
    var unitIndex = 0
    while (size >= TRAFFIC_UNIT_STEP && unitIndex < units.size - 1) {
        size /= TRAFFIC_UNIT_STEP
        unitIndex++
    }
    return String.format(Locale.getDefault(), "%.1f %s", size, units[unitIndex])
}

val URI.idnHost: String
    get() = host?.replace("[", "")?.replace("]", "").orEmpty()

fun String?.removeWhiteSpace(): String? = this?.replace(" ", "")

fun String?.nullIfBlank(): String? = this?.takeIf { it.isNotBlank() }

fun String.toLongOrZero(): Long = toLongOrNull() ?: 0L

/**
 * Registers for package add/remove. If [unregisterAfterFirst] is true, unregisters after the first callback.
 */
fun Context.listenForPackageChanges(
    unregisterAfterFirst: Boolean = true,
    callback: () -> Unit,
) = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        callback()
        if (unregisterAfterFirst) context.unregisterReceiver(this)
    }
}.apply {
    val filter = IntentFilter().apply {
        addAction(Intent.ACTION_PACKAGE_ADDED)
        addAction(Intent.ACTION_PACKAGE_REMOVED)
        addDataScheme("package")
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        registerReceiver(this, filter, Context.RECEIVER_EXPORTED)
    } else {
        registerReceiver(this, filter)
    }
}

inline fun <reified T : Serializable> Bundle.serializable(key: String): T? =
    BundleCompat.getSerializable(this, key, T::class.java)

inline fun <reified T : Serializable> Intent.serializable(key: String): T? =
    IntentCompat.getSerializableExtra(this, key, T::class.java)

fun CharSequence?.isNotNullEmpty(): Boolean = !isNullOrBlank()

fun String.concatUrl(vararg paths: String): String {
    val builder = StringBuilder(trimEnd('/'))
    paths.forEach { path ->
        val trimmed = path.trim('/')
        if (trimmed.isNotEmpty()) {
            builder.append('/').append(trimmed)
        }
    }
    return builder.toString()
}
