package com.thindie.engine.core

import java.util.Date

/**
 * Custom logging object to replace standard Android Log calls for better control and abstraction.
 */
object Log {
  const val LOG_TAG = "[ApplicationTag]"

  /**
   * Severity levels, ordered from least to most severe.
   * Every line is prefixed with the level tag so output can be filtered later (e.g., in logcat).
   */
  enum class Level(val tag: String) {
    VERBOSE("V"),
    DEBUG("D"),
    INFO("I"),
    WARN("W"),
    ERROR("E"),
  }

  /**
   * Sink interface for receiving structured log entries.
   * Register sinks via [addSink] to capture logs programmatically (e.g., for UI display).
   */
  fun interface LoggingSink {
    fun log(entry: LogEntry)
  }

  private val sinks = mutableListOf<LoggingSink>()

  /**
   * Registers a sink that will receive all subsequent log entries.
   */
  @JvmStatic
  fun addSink(sink: LoggingSink) {
    synchronized(sinks) {
      sinks.add(sink)
    }
  }

  /**
   * Removes a previously registered sink.
   */
  @JvmStatic
  fun removeSink(sink: LoggingSink) {
    synchronized(sinks) {
      sinks.remove(sink)
    }
  }

  private fun dispatch(
    level: Level,
    tag: String,
    message: String,
  ) {
    val entry = LogEntry(Date(), level, tag, message)
    synchronized(sinks) {
      for (sink in sinks) {
        sink.log(entry)
      }
    }
  }

  /**
   * Logs a VERBOSE-level entry.
   * @param throwable Optional exception; its full stack trace is appended to the message.
   */
  @JvmStatic
  fun v(
    message: () -> String,
    tag: String = LOG_TAG,
    throwable: Throwable? = null,
  ) {
    log(Level.VERBOSE, tag, message, throwable)
  }

  /**
   * Logs a DEBUG-level entry.
   * @param throwable Optional exception; its full stack trace is appended to the message.
   */
  @JvmStatic
  fun d(
    message: () -> String,
    tag: String = LOG_TAG,
    throwable: Throwable? = null,
  ) {
    log(Level.DEBUG, tag, message, throwable)
  }

  /**
   * Logs an INFO-level entry.
   * @param throwable Optional exception; its full stack trace is appended to the message.
   */
  @JvmStatic
  fun i(
    message: () -> String,
    tag: String = LOG_TAG,
    throwable: Throwable? = null,
  ) {
    log(Level.INFO, tag, message, throwable)
  }

  /**
   * Logs a WARN-level entry.
   * @param throwable Optional exception; its full stack trace is appended to the message.
   */
  @JvmStatic
  fun w(
    message: () -> String,
    tag: String = LOG_TAG,
    throwable: Throwable? = null,
  ) {
    log(Level.WARN, tag, message, throwable)
  }

  /**
   * Logs an ERROR-level entry.
   * @param throwable Optional exception; its full stack trace is appended to the message.
   */
  @JvmStatic
  fun e(
    message: () -> String,
    tag: String = LOG_TAG,
    throwable: Throwable? = null,
  ) {
    log(Level.ERROR, tag, message, throwable)
  }

  private fun log(
    level: Level,
    tag: String,
    message: () -> String,
    throwable: Throwable?,
  ) {
    val msg = message.invoke()
    if (throwable != null) {
      println("$tag ${level.tag} $msg\n" + throwable.stackTraceToString())
      dispatch(level, tag, "$msg\n${throwable.stackTraceToString()}")
    } else {
      println("$tag ${level.tag} $msg")
      dispatch(level, tag, msg)
    }
  }
}
