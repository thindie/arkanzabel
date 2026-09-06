package com.thindie.rknzbl.application.work

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.annotations.TestOnly

class GlobalJobManager(private val coroutineScope: CoroutineScope) {
  private val jobs = MutableStateFlow<Map<Any, Job>>(mutableMapOf())

  fun isRunning(key: Any): Flow<Boolean> = jobs.map { it[key] != null }

  @TestOnly
  fun isRunningSync(key: Any) = jobs.value[key] != null

  fun launchGlobal(
    key: Any,
    block: suspend () -> Unit,
  ) {
    if (jobs.value.containsKey(key)) return
    val job = coroutineScope.launch { block() }
    jobs.update { current ->
      if (current.containsKey(key)) return@update current else current + (key to job)
    }
    // Registered after insertion: for an already-completed job this fires immediately,
    // so a fast-failing task never leaves a stale entry behind.
    job.invokeOnCompletion {
      jobs.update { current ->
        if (current[key] === job) current - key else current
      }
    }
  }

  fun cancel(key: Any) {
    val job = jobs.value[key] ?: return
    job.cancel()
    // Remove synchronously so a relaunch with the same key is not blocked by the dying entry.
    jobs.update { current ->
      if (current[key] === job) current - key else current
    }
  }
}
