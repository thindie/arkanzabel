package com.thindie.engine.core

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class RouterScopeTest {
  private data class TestState(val value: Int) : ViewState

  private sealed interface TestCommand : Command {
    data class SetValue(val v: Int) : TestCommand

    data object NoOp : TestCommand
  }

  private fun makeRoute(
    onScope: (ScreenScope<TestState, TestCommand>) -> Unit,
    execute: suspend (TestCommand, TestState) -> TestState?,
  ): Route =
    RouteFactory.create<TestCommand, TestState>(
      id = "test",
      initialState = TestState(0),
      execute = execute,
      stateSink = onScope,
      routeContent = {},
    )

  @Test
  fun stateSink_receivesScopeThatReflectsCommandUpdates() =
    runBlocking {
      var captured: ScreenScope<TestState, TestCommand>? = null
      val done = CompletableDeferred<Unit>()
      makeRoute(
        onScope = { captured = it },
        execute = { cmd, _ ->
          if (cmd is TestCommand.SetValue) {
            done.complete(Unit)
            TestState(cmd.v)
          } else {
            null
          }
        },
      )

      val scope = captured ?: fail("stateSink was never invoked")
      scope.send(TestCommand.SetValue(42))
      done.await()

      assertEquals(42, scope.state.value.value)
    }

  @Test
  fun execute_returnsNull_stateIsNotWritten() =
    runBlocking {
      var captured: ScreenScope<TestState, TestCommand>? = null
      val valueDone = CompletableDeferred<Unit>()
      val noopDone = CompletableDeferred<Unit>()
      makeRoute(
        onScope = { captured = it },
        execute = { cmd, _ ->
          when (cmd) {
            is TestCommand.SetValue -> {
              valueDone.complete(Unit)
              TestState(cmd.v)
            }
            TestCommand.NoOp -> {
              noopDone.complete(Unit)
              null
            }
          }
        },
      )

      val scope = captured ?: fail("stateSink was never invoked")

      scope.send(TestCommand.SetValue(99))
      valueDone.await()
      assertEquals(99, scope.state.value.value)

      scope.send(TestCommand.NoOp)
      noopDone.await()
      assertEquals(99, scope.state.value.value, "null-результат execute must not change the state")
    }
}
