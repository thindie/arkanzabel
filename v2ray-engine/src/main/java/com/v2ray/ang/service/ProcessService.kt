package com.v2ray.ang.service

import android.content.Context
import com.thindie.engine.core.Log
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
    Log.i({ cmd.toString() }, AppConfig.TAG)
    shutdownWaitExecutor()
    waitExecutor = Executors.newSingleThreadExecutor()
    try {
      val proBuilder = ProcessBuilder(cmd)
      proBuilder.redirectErrorStream(true)
      process =
        proBuilder
          .directory(context.filesDir)
          .start()
      Log.i({ process.toString() }, AppConfig.TAG)
      waitExecutor?.execute {
        try {
          val code = process?.waitFor()
          Log.i({ "runProcess exited with code $code" }, AppConfig.TAG)
        } catch (e: InterruptedException) {
          Thread.currentThread().interrupt()
          Log.w({ "runProcess wait interrupted" }, AppConfig.TAG, e)
        }
      }
    } catch (e: IOException) {
      Log.e({ "runProcess start failed" }, AppConfig.TAG, e)
      shutdownWaitExecutor()
      process = null
    }
  }

  /** Stops the running process and tears down the wait thread. */
  fun stopProcess() {
    Log.i({ "runProcess destroy" }, AppConfig.TAG)
    try {
      process?.destroy()
    } catch (e: SecurityException) {
      Log.e({ "runProcess destroy denied" }, AppConfig.TAG, e)
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
