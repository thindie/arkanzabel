package com.v2ray.ang.service

import android.content.Context
import android.util.Log
import com.v2ray.ang.AppConfig
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ProcessService {
  private var process: Process? = null
  private var waitExecutor: ExecutorService? = null

  /**
   * Runs a process with the given command. [Process.waitFor] runs on a single background thread
   * so the caller is not blocked.
   */
  fun runProcess(
    context: Context,
    cmd: List<String>,
  ) {
    Log.i(AppConfig.TAG, cmd.toString())
    shutdownWaitExecutor()
    waitExecutor = Executors.newSingleThreadExecutor()
    try {
      val proBuilder = ProcessBuilder(cmd)
      proBuilder.redirectErrorStream(true)
      process =
        proBuilder
          .directory(context.filesDir)
          .start()
      Log.i(AppConfig.TAG, process.toString())
      waitExecutor?.execute {
        try {
          val code = process?.waitFor()
          Log.i(AppConfig.TAG, "runProcess exited with code $code")
        } catch (e: InterruptedException) {
          Thread.currentThread().interrupt()
          Log.w(AppConfig.TAG, "runProcess wait interrupted", e)
        }
      }
    } catch (e: IOException) {
      Log.e(AppConfig.TAG, "runProcess start failed", e)
      shutdownWaitExecutor()
      process = null
    }
  }

  /** Stops the running process and tears down the wait thread. */
  fun stopProcess() {
    Log.i(AppConfig.TAG, "runProcess destroy")
    try {
      process?.destroy()
    } catch (e: SecurityException) {
      Log.e(AppConfig.TAG, "runProcess destroy denied", e)
    } finally {
      shutdownWaitExecutor()
      process = null
    }
  }

  private fun shutdownWaitExecutor() {
    waitExecutor?.shutdownNow()
    waitExecutor = null
  }
}
