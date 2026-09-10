package com.thindie.rknzbl.application.work

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GlobalJobManagerTest {
  @Test
  fun launchGlobal_tracksJobWhileRunning() =
    runTest {
      val manager = GlobalJobManager(this)
      val done = CompletableDeferred<Unit>()

      assertFalse(manager.isRunningSync("key"))

      manager.launchGlobal("key") { done.await() }
      assertTrue(manager.isRunningSync("key"), "job must be tracked right after launchGlobal")

      done.complete(Unit)
      advanceUntilIdle()
      assertFalse(manager.isRunningSync("key"), "entry must be removed once the job completes")
    }

  @Test
  fun launchGlobal_ignoresDuplicateKeyWhileFirstIsRunning() =
    runTest {
      val manager = GlobalJobManager(this)
      val runs = mutableListOf<Unit>()
      val done = CompletableDeferred<Unit>()

      manager.launchGlobal("key") {
        runs.add(Unit)
        done.await()
      }
      manager.launchGlobal("key") { runs.add(Unit) }

      advanceUntilIdle()

      assertEquals(1, runs.size, "second launch with the same key must be ignored while the first is running")

      done.complete(Unit)
      advanceUntilIdle()
    }

  @Test
  fun launchGlobal_allowsRelaunchAfterCompletion() =
    runTest {
      val manager = GlobalJobManager(this)
      val runs = mutableListOf<Unit>()

      manager.launchGlobal("key") { runs.add(Unit) }
      advanceUntilIdle()

      manager.launchGlobal("key") { runs.add(Unit) }
      advanceUntilIdle()

      assertEquals(2, runs.size, "same key must be launchable again after the previous job finished")
    }

  @Test
  fun launchGlobal_tracksKeysIndependently() =
    runTest {
      val manager = GlobalJobManager(this)
      val doneA = CompletableDeferred<Unit>()

      manager.launchGlobal("a") { doneA.await() }
      manager.launchGlobal("b") { }

      advanceUntilIdle()

      assertTrue(manager.isRunningSync("a"), "still-running job must stay tracked")
      assertFalse(manager.isRunningSync("b"), "completed job must not be tracked")

      doneA.complete(Unit)
      advanceUntilIdle()
    }

  @Test
  fun launchGlobal_removesEntryWhenBlockThrows() =
    runTest {
      val captured = CompletableDeferred<Throwable>()
      // A separate scope: an uncaught exception in the TestScope would fail this test itself.
      val scope =
        CoroutineScope(
          SupervisorJob() + Dispatchers.Unconfined +
            CoroutineExceptionHandler { _, throwable -> captured.complete(throwable) },
        )
      val manager = GlobalJobManager(scope)

      manager.launchGlobal("key") { throw IllegalStateException("boom") }

      assertTrue(captured.isCompleted, "exception must reach the handler")
      assertFalse(manager.isRunningSync("key"), "failed job must be removed from tracking")
    }

  @Test
  fun cancel_stopsRunningJobAndRemovesEntry() =
    runTest {
      val manager = GlobalJobManager(this)
      val done = CompletableDeferred<Unit>()
      val runs = mutableListOf<Unit>()

      manager.launchGlobal("key") {
        runs.add(Unit)
        done.await()
      }
      advanceUntilIdle()
      assertTrue(manager.isRunningSync("key"))

      manager.cancel("key")
      advanceUntilIdle()

      assertFalse(manager.isRunningSync("key"), "cancelled job must be removed from tracking")
      assertEquals(1, runs.size, "block must not run again after cancel")
    }

  @Test
  fun cancel_allowsImmediateRelaunchWithSameKey() =
    runTest {
      val manager = GlobalJobManager(this)
      val done = CompletableDeferred<Unit>()
      val runs = mutableListOf<Int>()

      manager.launchGlobal("key") {
        runs.add(1)
        done.await()
      }
      advanceUntilIdle()
      manager.cancel("key")

      // Relaunch right away, like pull-to-refresh restart does.
      manager.launchGlobal("key") { runs.add(2) }
      advanceUntilIdle()

      assertEquals(listOf(1, 2), runs, "relaunch after cancel must not be swallowed by the stale entry")
      assertFalse(manager.isRunningSync("key"), "second job completed and was cleaned up")
    }

  @Test
  fun cancel_isNoopWhenKeyIsNotRunning() =
    runTest {
      val manager = GlobalJobManager(this)

      manager.cancel("key")

      assertFalse(manager.isRunningSync("key"))
    }

  @Test
  fun isRunning_flowReflectsJobLifecycle() =
    runTest {
      val manager = GlobalJobManager(this)
      val emissions = mutableListOf<Boolean>()
      val done = CompletableDeferred<Unit>()

      backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
        manager.isRunningCold("key").take(3).collect { emissions.add(it) }
      }

      advanceUntilIdle()
      assertEquals(listOf(false), emissions, "no job yet - flow must emit false")

      manager.launchGlobal("key") { done.await() }
      advanceUntilIdle()
      assertEquals(listOf(false, true), emissions, "flow must emit true while the job is running")

      done.complete(Unit)
      advanceUntilIdle()
      assertEquals(listOf(false, true, false), emissions, "flow must emit false once the job completes")
    }
}
