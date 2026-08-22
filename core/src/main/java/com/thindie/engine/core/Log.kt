package com.thindie.engine.core

/**
 * Custom logging object to replace standard Android Log calls for better control and abstraction.
 */
object Log {
  const val LOG_TAG = "[ApplicationTag]"

  @JvmStatic
  inline fun d(
    message: () -> String,
    tag: String = LOG_TAG,
  ) {
    println(tag + message.invoke())
  }

  /**
   * Logs an error message at the ERROR level, printing it to logcat.
   * @param tag The log tag.
   * @param message The error message content to log.
   * @param throwable Optional exception associated with the error.
   */
  @JvmStatic
  inline fun e(
    message: () -> String,
    tag: String = LOG_TAG,
    throwable: Throwable? = null,
  ) {
    if (throwable != null) {
      println(tag + " " + message.invoke() + " " + throwable.message)
    } else {
      println("$tag ${message.invoke()}")
    }
  }

  /**
   * Logs a warning message at the WARN level, printing it to logcat.
   * @param tag The log tag.
   * @param message The warning message content to log.
   */
  @JvmStatic
  inline fun w(
    message: () -> String,
    tag: String = LOG_TAG,
  ) {
    println("$tag ${message.invoke()}")
  }
}
