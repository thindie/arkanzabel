package com.thindie.rknzbl.application

import com.thindie.engine.core.Log
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LogSinkProviderTest {
  private val kernelTag = "com.thindie.rknzbl"
  private val debugTag = "some-other-tag"

  @Before
  fun setUp() {
    // Reset singleton state before each test
    LogSinkProvider.init()
  }

  @After
  fun tearDown() {
    // Clear flows after each test to avoid cross-test pollution
    LogSinkProvider.clearByLevel(null)
    LogSinkProvider.clearKernelByLevel(null)
  }

  @Test
  fun kernel_log_routes_to_kernel_flow_only() = runTest {
    Log.i({ "kernel message" }, kernelTag)

    assertEquals(1, LogSinkProvider.kernelEntries.value.size)
    assertEquals("kernel message", LogSinkProvider.kernelEntries.value[0].message)
    assertEquals(0, LogSinkProvider.entries.value.size)
  }

  @Test
  fun debug_log_routes_to_debug_flow_only() = runTest {
    Log.i({ "debug message" }, debugTag)

    assertEquals(1, LogSinkProvider.entries.value.size)
    assertEquals("debug message", LogSinkProvider.entries.value[0].message)
    assertEquals(0, LogSinkProvider.kernelEntries.value.size)
  }

  @Test
  fun mixed_logs_route_correctly() = runTest {
    Log.i({ "kernel msg" }, kernelTag)
    Log.d({ "debug msg" }, debugTag)
    Log.w({ "another kernel" }, kernelTag)

    assertEquals(2, LogSinkProvider.kernelEntries.value.size)
    assertEquals("kernel msg", LogSinkProvider.kernelEntries.value[0].message)
    assertEquals("another kernel", LogSinkProvider.kernelEntries.value[1].message)

    assertEquals(1, LogSinkProvider.entries.value.size)
    assertEquals("debug msg", LogSinkProvider.entries.value[0].message)
  }

  @Test
  fun clear_by_level_removes_matching_entries() = runTest {
    Log.i({ "info debug" }, debugTag)
    Log.e({ "error debug" }, debugTag)
    Log.w({ "warn debug" }, debugTag)

    assertEquals(3, LogSinkProvider.entries.value.size)

    LogSinkProvider.clearByLevel(Log.Level.ERROR)

    assertEquals(2, LogSinkProvider.entries.value.size)
    assertTrue(LogSinkProvider.entries.value.none { it.level == Log.Level.ERROR })
  }

  @Test
  fun clear_all_debug_entries() = runTest {
    Log.i({ "msg1" }, debugTag)
    Log.e({ "msg2" }, debugTag)

    assertEquals(2, LogSinkProvider.entries.value.size)

    LogSinkProvider.clearByLevel(null)

    assertEquals(0, LogSinkProvider.entries.value.size)
  }

  @Test
  fun clear_kernel_by_level_removes_matching_entries() = runTest {
    Log.i({ "info kernel" }, kernelTag)
    Log.e({ "error kernel" }, kernelTag)
    Log.w({ "warn kernel" }, kernelTag)

    assertEquals(3, LogSinkProvider.kernelEntries.value.size)

    LogSinkProvider.clearKernelByLevel(Log.Level.ERROR)

    assertEquals(2, LogSinkProvider.kernelEntries.value.size)
    assertTrue(LogSinkProvider.kernelEntries.value.none { it.level == Log.Level.ERROR })
  }

  @Test
  fun clear_all_kernel_entries() = runTest {
    Log.i({ "msg1" }, kernelTag)
    Log.e({ "msg2" }, kernelTag)

    assertEquals(2, LogSinkProvider.kernelEntries.value.size)

    LogSinkProvider.clearKernelByLevel(null)

    assertEquals(0, LogSinkProvider.kernelEntries.value.size)
  }

  @Test
  fun clear_debug_does_not_affect_kernel() = runTest {
    Log.i({ "debug msg" }, debugTag)
    Log.i({ "kernel msg" }, kernelTag)

    LogSinkProvider.clearByLevel(null)

    assertEquals(0, LogSinkProvider.entries.value.size)
    assertEquals(1, LogSinkProvider.kernelEntries.value.size)
  }

  @Test
  fun clear_kernel_does_not_affect_debug() = runTest {
    Log.i({ "debug msg" }, debugTag)
    Log.i({ "kernel msg" }, kernelTag)

    LogSinkProvider.clearKernelByLevel(null)

    assertEquals(1, LogSinkProvider.entries.value.size)
    assertEquals(0, LogSinkProvider.kernelEntries.value.size)
  }
}
