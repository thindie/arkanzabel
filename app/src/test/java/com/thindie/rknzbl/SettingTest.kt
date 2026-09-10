package com.thindie.rknzbl

import com.thindie.engine.core.Command
import com.thindie.engine.core.Route
import com.thindie.engine.core.RouteFactory
import com.thindie.engine.core.ScreenScope
import com.thindie.engine.core.ViewState
import com.thindie.engine.core.sub
import com.thindie.engine.core.transition
import com.thindie.rknzbl.appfeatures.settings.data.Setting
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Test
import kotlin.test.assertEquals

class SettingTest {
  private data class TestState(val value: Boolean? = null) : ViewState

  data object TestCommand : Command

  private fun makeRoute(
    setting: Setting<Boolean>,
    onScope: (ScreenScope<TestState, TestCommand>) -> Unit,
    execute: suspend (TestCommand, TestState) -> TestState?,
    onContent: (ScreenScope<TestState, TestCommand>) -> Unit,
  ): Route =
    RouteFactory.create<TestCommand, TestState>(
      id = "test",
      initialState = TestState(),
      execute = execute,
      stateSink = onScope,
      routeContent = { onContent(it) },
    )

  // The sink collects on Dispatchers.Default (RouteFactory), so wait in real time.
  private fun awaitValue(
    scope: ScreenScope<TestState, TestCommand>,
    expected: Boolean?,
  ) {
    val deadline = System.currentTimeMillis() + 2_000
    while (scope.state.value.value != expected) {
      check(System.currentTimeMillis() < deadline) { "Timed out waiting for state value $expected" }
      Thread.sleep(10)
    }
  }

  @Test
  fun setting_emits_non_null() {
    val casCache = MutableStateFlow<Boolean?>(null)
    val setting =
      Setting<Boolean>(
        read = { casCache.value },
        write = { v -> casCache.value = v },
      )
    val scopes = mutableListOf<ScreenScope<TestState, TestCommand>>()

    makeRoute(
      setting = setting,
      onScope = { scope ->
        scopes.add(scope)
        scope.sub(setting.flow).transition { state, bool -> state.copy(value = bool) }
      },
      execute = { _, _ -> null },
      onContent = {},
    )

    val scope = scopes.single()
    // While the value is null, the flow must not emit anything.
    assertEquals(null, scope.state.value.value)

    setting.set(true)
    awaitValue(scope, true)
  }

  @Test
  fun state_sink_collects_properly() {
    val casCache = MutableStateFlow<Boolean?>(true)
    val setting =
      Setting<Boolean>(
        read = { casCache.value },
        write = { v -> casCache.value = v },
      )
    val scopes = mutableListOf<ScreenScope<TestState, TestCommand>>()

    makeRoute(
      setting = setting,
      onScope = { scope ->
        scopes.add(scope)
        scope.sub(setting.flow).transition { state, bool -> state.copy(value = bool) }
      },
      execute = { _, _ -> null },
      onContent = {},
    )

    val scope = scopes.single()
    awaitValue(scope, true)
    assertEquals(true, scope.state.value.value)
  }
}
