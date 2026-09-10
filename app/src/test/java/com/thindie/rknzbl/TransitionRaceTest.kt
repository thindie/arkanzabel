package com.thindie.rknzbl

import com.thindie.engine.core.Command
import com.thindie.engine.core.RouteFactory
import com.thindie.engine.core.ScreenScope
import com.thindie.engine.core.ViewState
import com.thindie.engine.core.sub
import com.thindie.engine.core.transition
import com.thindie.rknzbl.appfeatures.settings.data.Setting
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Regression test: several sink collectors start in parallel on Dispatchers.Default when a route
 * is created. Each one does a read-modify-write of the screen state; without serialization the
 * last writer clobbers fields written by the others (e.g. "new design" vs custom source).
 */
class TransitionRaceTest {
  private data class TestState(
    val a: Boolean? = null,
    val b: Boolean? = null,
    val c: Boolean? = null,
    val d: Boolean? = null,
    val e: Boolean? = null,
  ) : ViewState

  private sealed interface TestCommand : Command {
    data object NoOp : TestCommand
  }

  @Test
  fun parallel_transitions_do_not_lose_fields() {
    // One shared repository per test run, like the real app: settings keep their in-memory value
    // across route recreations (fast tab switching), while each new scope starts from nulls.
    val settings =
      listOf(true, false, true, false, true).map { initial ->
        Setting<Boolean>(read = { initial }, write = {})
      }

    repeat(20) { iteration ->
      val scopes = mutableListOf<ScreenScope<TestState, TestCommand>>()

      RouteFactory.create<TestCommand, TestState>(
        id = "race",
        initialState = TestState(),
        execute = { _, _ -> null },
        stateSink = { scope ->
          scopes.add(scope)
          scope.sub(settings[0].flow).transition { s, v -> s.copy(a = v) }
          scope.sub(settings[1].flow).transition { s, v -> s.copy(b = v) }
          scope.sub(settings[2].flow).transition { s, v -> s.copy(c = v) }
          scope.sub(settings[3].flow).transition { s, v -> s.copy(d = v) }
          scope.sub(settings[4].flow).transition { s, v -> s.copy(e = v) }
        },
        routeContent = {},
      )

      val scope = scopes.single()
      val deadline = System.currentTimeMillis() + 5_000
      while (true) {
        val state = scope.state.value
        if (state.a != null && state.b != null && state.c != null && state.d != null) break
        check(System.currentTimeMillis() < deadline) { "Timed out on iteration $iteration: $state" }
        Thread.sleep(10)
      }

      val state = scope.state.value
      assertEquals(true, state.a, "iteration=$iteration")
      assertEquals(false, state.b, "iteration=$iteration")
      assertEquals(true, state.c, "iteration=$iteration")
      assertEquals(false, state.d, "iteration=$iteration")
    }
  }

  @Test
  fun setting_set_before_subscribe_is_not_lost() {
    val cache = MutableStateFlow<Boolean?>(null)
    val setting = Setting<Boolean>(read = { cache.value }, write = { v -> cache.value = v })
    // Simulate a toggle made while no collector is attached.
    setting.set(true)

    val scopes = mutableListOf<ScreenScope<TestState, TestCommand>>()
    RouteFactory.create<TestCommand, TestState>(
      id = "late",
      initialState = TestState(),
      execute = { _, _ -> null },
      stateSink = { scope ->
        scopes.add(scope)
        scope.sub(setting.flow).transition { s, v -> s.copy(a = v) }
      },
      routeContent = {},
    )

    val scope = scopes.single()
    val deadline = System.currentTimeMillis() + 5_000
    while (scope.state.value.a != true) {
      check(System.currentTimeMillis() < deadline) { "Timed out: ${scope.state.value}" }
      Thread.sleep(10)
    }
    // The write happens in onEach right after the emission is delivered; give it a moment.
    val writeDeadline = System.currentTimeMillis() + 2_000
    while (cache.value != true) {
      check(System.currentTimeMillis() < writeDeadline) { "Timed out waiting for persistence: ${scope.state.value}" }
      Thread.sleep(10)
    }
  }
}
