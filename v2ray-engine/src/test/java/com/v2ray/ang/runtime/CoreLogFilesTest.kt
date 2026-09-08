package com.v2ray.ang.runtime

import com.thindie.engine.core.Log
import com.v2ray.ang.AppConfig
import java.util.Calendar
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Tests for the Xray kernel file-log line parser (Go `log.Ldate|log.Ltime` format).
 */
class CoreLogFilesTest {
  @Test
  fun parse_debugLine() {
    val entry =
      assertNotNull(
        CoreLogFiles.parseCoreLogLine(
          "2026/09/08 14:03:22 [Debug] app/proxymanager: proxy [tun >> proxy] accepted tcp:1.2.3.4:5678",
        ),
      )
    assertEquals(Log.Level.DEBUG, entry.level)
    assertEquals(AppConfig.TAG_KERNEL, entry.tag)
    assertEquals(
      "app/proxymanager: proxy [tun >> proxy] accepted tcp:1.2.3.4:5678",
      entry.message,
    )
  }

  @Test
  fun parse_infoLine() {
    val entry =
      assertNotNull(
        CoreLogFiles.parseCoreLogLine("2026/09/08 14:03:22 [Info] app/dispatcher: sniffed domain: example.com"),
      )
    assertEquals(Log.Level.INFO, entry.level)
    assertEquals("app/dispatcher: sniffed domain: example.com", entry.message)
  }

  @Test
  fun parse_warningLine() {
    val entry =
      assertNotNull(
        CoreLogFiles.parseCoreLogLine("2026/09/08 14:03:22 [Warning] app/proxyman: dial failed"),
      )
    assertEquals(Log.Level.WARN, entry.level)
    assertEquals("app/proxyman: dial failed", entry.message)
  }

  @Test
  fun parse_errorLine() {
    val entry =
      assertNotNull(
        CoreLogFiles.parseCoreLogLine("2026/09/08 14:03:22 [Error] app/dns: resolve timeout"),
      )
    assertEquals(Log.Level.ERROR, entry.level)
    assertEquals("app/dns: resolve timeout", entry.message)
  }

  @Test
  fun parse_lineWithoutLevelTag_defaultsToInfo() {
    val entry =
      assertNotNull(
        CoreLogFiles.parseCoreLogLine("2026/09/08 14:03:22 plain message without level"),
      )
    assertEquals(Log.Level.INFO, entry.level)
    assertEquals("plain message without level", entry.message)
  }

  @Test
  fun parse_malformedTimestamp_returnsNull() {
    assertNull(CoreLogFiles.parseCoreLogLine("not a timestamp [Debug] something"))
    assertNull(CoreLogFiles.parseCoreLogLine(""))
  }

  @Test
  fun parse_timestampIsParsed() {
    val entry = assertNotNull(CoreLogFiles.parseCoreLogLine("2026/09/08 14:03:22 [Debug] msg"))
    val calendar = Calendar.getInstance().apply { time = entry.timestamp }
    assertEquals(2026, calendar.get(Calendar.YEAR))
    assertEquals(Calendar.SEPTEMBER, calendar.get(Calendar.MONTH))
    assertEquals(8, calendar.get(Calendar.DAY_OF_MONTH))
  }
}
