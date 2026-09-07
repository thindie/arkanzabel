package com.thindie.rknzbl.application

import com.thindie.engine.core.Log
import com.thindie.engine.core.LogEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Singleton that registers a LoggingSink with the core Log object and exposes
 * collected entries as a StateFlow for UI consumption.
 */
object LogSinkProvider {
  private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())
  val entries: StateFlow<List<LogEntry>> = _entries.asStateFlow()

  private var initialized = false

  /**
   * Initialize the sink provider by registering with core Log.
   * Call once at app startup (e.g., in MainActivity.onCreate).
   */
  fun init() {
    if (initialized) return
    initialized = true

    Log.addSink { entry ->
      _entries.update { current ->
        val combined = current + entry
        // Keep last 500 entries
        if (combined.size > 500) {
          combined.takeLast(500)
        } else {
          combined
        }
      }
    }
  }

  /**
   * Clears all collected log entries of the given level, or everything when [level] is null.
   */
  fun clearByLevel(level: Log.Level?) {
    if (level == null) {
      _entries.value = emptyList()
    } else {
      _entries.update { current -> current.filterNot { it.level == level } }
    }
  }
}
