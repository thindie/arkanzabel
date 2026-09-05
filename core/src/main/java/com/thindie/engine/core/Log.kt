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

  private fun dispatch(level: Level, tag: String, message: String) {
    val entry = LogEntry(Date(), level, tag, message)
    synchronized(sinks) {
      for (sink in sinks) {
        sink.log(entry)
      }
    }
  }

  @JvmStatic
  fun v(
    message: () -> String,
    tag: String = LOG_TAG,
  ) {
    val msg = message.invoke()
    println("$tag ${Level.VERBOSE.tag} $msg")
    dispatch(Level.VERBOSE, tag, msg)
  }

  @JvmStatic
  fun d(
    message: () -> String,
    tag: String = LOG_TAG,
  ) {
    val msg = message.invoke()
    println("$tag ${Level.DEBUG.tag} $msg")
    dispatch(Level.DEBUG, tag, msg)
  }

  @JvmStatic
  fun i(
    message: () -> String,
    tag: String = LOG_TAG,
  ) {
    val msg = message.invoke()
    println("$tag ${Level.INFO.tag} $msg")
    dispatch(Level.INFO, tag, msg)
  }

  /**
   * Logs an error message at the ERROR level, printing it to logcat.
   * @param tag The log tag.
   * @param message The error message content to log.
   * @param throwable Optional exception associated with the error; its full stack trace is printed.
   */
  @JvmStatic
  fun e(
    message: () -> String,
    tag: String = LOG_TAG,
    throwable: Throwable? = null,
  ) {
    val msg = message.invoke()
    if (throwable != null) {
      println("$tag ${Level.ERROR.tag} $msg\n" + throwable.stackTraceToString())
      dispatch(Level.ERROR, tag, "$msg\n${throwable.stackTraceToString()}")
    } else {
      println("$tag ${Level.ERROR.tag} $msg")
      dispatch(Level.ERROR, tag, msg)
    }
  }

  @JvmStatic
  fun w(
    message: () -> String,
    tag: String = LOG_TAG,
  ) {
    val msg = message.invoke()
    println("$tag ${Level.WARN.tag} $msg")
    dispatch(Level.WARN, tag, msg)
  }
}
