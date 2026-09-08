package com.v2ray.ang.runtime

import android.content.Context
import com.thindie.engine.core.Log
import com.thindie.engine.core.LogEntry
import com.v2ray.ang.AppConfig
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Manages the Xray kernel's file-based logs (access + error).
 * The core writes them with Go `log.Ldate|log.Ltime` format:
 * `2026/09/08 14:03:22 [Debug] app/proxymanager: ...`.
 */
object CoreLogFiles {
  private const val ACCESS_FILE_NAME = "v2ray-access.log"
  private const val ERROR_FILE_NAME = "v2ray-error.log"

  fun accessFile(context: Context): File = File(context.filesDir, ACCESS_FILE_NAME)

  fun errorFile(context: Context): File = File(context.filesDir, ERROR_FILE_NAME)

  /**
   * Truncates both log files. Called at session start before the core launches so each
   * VPN/proxy session begins with a clean kernel log. Never throws — file errors must not
   * prevent core startup.
   */
  fun truncateAll(context: Context) {
    listOf(accessFile(context), errorFile(context)).forEach { file ->
      runCatching { if (file.exists()) file.writeText("") }
        .onFailure { Log.w({ "Failed to truncate ${file.name}" }, AppConfig.TAG_KERNEL, it) }
    }
  }

  /**
   * Reads and parses both log files. Blocking — call from [kotlinx.coroutines.Dispatchers.IO].
   */
  fun readEntries(context: Context): List<LogEntry> {
    val entries =
      listOf(accessFile(context), errorFile(context)).flatMap { file ->
        if (!file.exists()) {
          emptyList()
        } else {
          runCatching { file.readLines() }.getOrDefault(emptyList()).mapNotNull(::parseCoreLogLine)
        }
      }
    return entries.sortedBy { it.timestamp }
  }

  private val LINE_REGEX =
    Regex("""^(\d{4}/\d{2}/\d{2} \d{2}:\d{2}:\d{2})\s*(?:\[([A-Za-z]+)\]\s*)?(.*)$""")

  internal fun parseCoreLogLine(line: String): LogEntry? {
    val match = LINE_REGEX.find(line) ?: return null
    val (rawTimestamp, rawLevel, message) = match.destructured
    // SimpleDateFormat is not thread-safe — create per call.
    val timestamp =
      SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US).parse(rawTimestamp) ?: return null
    val level =
      when (rawLevel?.lowercase()) {
        "trace" -> Log.Level.VERBOSE
        "debug" -> Log.Level.DEBUG
        "info", "notice" -> Log.Level.INFO
        "warning" -> Log.Level.WARN
        "error" -> Log.Level.ERROR
        else -> Log.Level.INFO
      }
    return LogEntry(timestamp, level, AppConfig.TAG_KERNEL, message)
  }
}
