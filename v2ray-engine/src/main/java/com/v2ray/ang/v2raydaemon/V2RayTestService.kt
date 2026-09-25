package com.v2ray.ang.v2raydaemon

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.v2ray.ang.AppConfig
import com.v2ray.ang.extension.serializable
import com.v2ray.ang.ipc.BroadcastersHolder
import com.v2ray.ang.runtime.V2RayNativeManager
import com.v2ray.ang.service.RealPingWorkerService
import java.util.Collections

class V2RayTestService : Service() {
  // manage active batch workers so each batch is independent and cancellable
  private val activeWorkers = Collections.synchronizedList(mutableListOf<RealPingWorkerService>())

  /**
   * Initializes the V2Ray environment.
   */
  override fun onCreate() {
    super.onCreate()
    V2RayNativeManager.initCoreEnv(this)
  }

  /**
   * Binds the service.
   * @param intent The intent.
   * @return The binder.
   */
  override fun onBind(intent: Intent?): IBinder? {
    return null
  }

  /**
   * Cleans up resources when the service is destroyed.
   */
  override fun onDestroy() {
    super.onDestroy()
    // cancel any active workers
    val snapshot = ArrayList(activeWorkers)
    snapshot.forEach { it.cancel() }
    activeWorkers.clear()
  }

  /**
   * Handles the start command for the service.
   * @param intent The intent.
   * @param flags The flags.
   * @param startId The start ID.
   * @return The start mode.
   */
  override fun onStartCommand(
    intent: Intent?,
    flags: Int,
    startId: Int,
  ): Int {
    when (intent?.getIntExtra("key", 0)) {
      AppConfig.MSG_MEASURE_CONFIG -> {
        val guidsList = intent.serializable<ArrayList<String>>("content")
        if (!guidsList.isNullOrEmpty()) {
          val ipc = (applicationContext as BroadcastersHolder).daemonBroadcastReceiver
          val worker =
            RealPingWorkerService(
              context = this,
              guids = guidsList,
              daemonBroadcastReceiver = ipc,
            ) { _, instance ->
              activeWorkers.remove(instance)
            }
          activeWorkers.add(worker)
          worker.start()
        }
      }

      AppConfig.MSG_MEASURE_CONFIG_CANCEL -> {
        val snapshot = ArrayList(activeWorkers)
        snapshot.forEach { it.cancel() }
        activeWorkers.clear()
      }
    }
    return super.onStartCommand(intent, flags, startId)
  }
}
