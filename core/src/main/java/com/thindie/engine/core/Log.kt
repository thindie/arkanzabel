package com.thindie.engine.core

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

  @JvmStatic
  inline fun v(
    message: () -> String,
    tag: String = LOG_TAG,
  ) {
    println("$tag ${Level.VERBOSE.tag} " + message.invoke())
  }

  @JvmStatic
  inline fun d(
    message: () -> String,
    tag: String = LOG_TAG,
  ) {
    println("$tag ${Level.DEBUG.tag} " + message.invoke())
  }

  @JvmStatic
  inline fun i(
    message: () -> String,
    tag: String = LOG_TAG,
  ) {
    println("$tag ${Level.INFO.tag} " + message.invoke())
  }

  /**
   * Logs an error message at the ERROR level, printing it to logcat.
   * @param tag The log tag.
   * @param message The error message content to log.
   * @param throwable Optional exception associated with the error; its full stack trace is printed.
   */
  @JvmStatic
  inline fun e(
    message: () -> String,
    tag: String = LOG_TAG,
    throwable: Throwable? = null,
  ) {
    if (throwable != null) {
      println("$tag ${Level.ERROR.tag} " + message.invoke() + "\n" + throwable.stackTraceToString())
    } else {
      println("$tag ${Level.ERROR.tag} " + message.invoke())
    }
  }

  @JvmStatic
  inline fun w(
    message: () -> String,
    tag: String = LOG_TAG,
  ) {
    println("$tag ${Level.WARN.tag} " + message.invoke())
  }
}
