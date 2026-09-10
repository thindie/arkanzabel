package com.thindie.engine.core

import java.util.Date

/**
 * Structured log entry passed to [LoggingSink] instances.
 */
data class LogEntry(
  val timestamp: Date,
  val level: Log.Level,
  val tag: String,
  val message: String,
)
