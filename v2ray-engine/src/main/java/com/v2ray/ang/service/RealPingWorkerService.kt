package com.v2ray.ang.service

import android.content.Context
import com.thindie.engine.core.Log
import com.v2ray.ang.AppConfig
import com.v2ray.ang.error.AppError
import com.v2ray.ang.ipc.DaemonToMain
import com.v2ray.ang.ipc.IpcDaemonBroadcastReceiver
import com.v2ray.ang.runtime.SettingsManager
import com.v2ray.ang.runtime.V2RayNativeManager
import com.v2ray.ang.runtime.V2rayConfigManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * Worker that runs a batch of real-ping tests independently.
 * Each batch owns its own CoroutineScope/dispatcher and can be cancelled separately.
 */
class RealPingWorkerService(
  private val context: Context,
  private val guids: List<String>,
  private val daemonBroadcastReceiver: IpcDaemonBroadcastReceiver,
  private val onFinish: (status: String, RealPingWorkerService) -> Unit = { _, _ -> },
) {
  private val job = SupervisorJob()
  private val cpu = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
  private val dispatcher = Executors.newFixedThreadPool(cpu * 4).asCoroutineDispatcher()
  private val scope = CoroutineScope(job + dispatcher + CoroutineName("RealPingBatchWorker"))

  private val runningCount = AtomicInteger(0)
  private val totalCount = AtomicInteger(0)

  fun start() {
    val jobs =
      guids.map { guid ->
        totalCount.incrementAndGet()
        scope.launch {
          runningCount.incrementAndGet()
          try {
            val result = startRealPing(guid)
            daemonBroadcastReceiver.sendEvent(DaemonToMain.ConfigTestResult(guid, result))
          } finally {
            val count = totalCount.decrementAndGet()
            val left = runningCount.decrementAndGet()
            daemonBroadcastReceiver.sendEvent(
              DaemonToMain.ConfigTestProgress(left, count),
            )
          }
        }
      }

    scope.launch {
      try {
        joinAll(*jobs.toTypedArray())
        daemonBroadcastReceiver.sendEvent(DaemonToMain.ConfigTestFinished)
        onFinish("0", this@RealPingWorkerService)
      } catch (_: CancellationException) {
        daemonBroadcastReceiver.sendEvent(DaemonToMain.ConfigTestFinished)
        onFinish("-1", this@RealPingWorkerService)
      } finally {
        close()
      }
    }
  }

  fun cancel() {
    job.cancel()
  }

  private fun close() {
    try {
      dispatcher.close()
    } catch (_: RuntimeException) {
      // ignore
    }
  }

  private fun startRealPing(guid: String): Long {
    return try {
      val built = V2rayConfigManager.getV2rayConfig4Speedtest(context, guid)
      V2RayNativeManager.measureOutboundDelay(
        built.json,
        SettingsManager.getDelayTestUrl(),
      )
    } catch (cancel: CancellationException) {
      throw cancel
    } catch (appError: AppError) {
      Log.w({ "Speedtest config failed for $guid: ${appError.userReadable}" }, AppConfig.TAG, appError)
      -1L
    } catch (runtime: RuntimeException) {
      Log.w({ "Speedtest config failed for $guid" }, AppConfig.TAG, runtime)
      -1L
    }
  }
}
