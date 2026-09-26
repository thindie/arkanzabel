package com.v2ray.ang.runtime

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import androidx.core.content.ContextCompat
import com.thindie.engine.core.Log
import com.thindie.rknzbl.v2rayengine.R
import com.v2ray.ang.AppConfig
import com.v2ray.ang.contracts.ServiceControl
import com.v2ray.ang.dto.ConnectionProfile
import com.v2ray.ang.enums.Protocol
import com.v2ray.ang.error.AppError
import com.v2ray.ang.ipc.BroadcastersHolder
import com.v2ray.ang.ipc.DaemonToMain
import com.v2ray.ang.ipc.FromMainToDaemon
import com.v2ray.ang.util.Utils
import com.v2ray.ang.v2raydaemon.V2RayProxyOnlyService
import com.v2ray.ang.v2raydaemon.V2RayVpnService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import java.lang.ref.SoftReference
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

object V2RayServiceManager {
  private const val STOP_LOOP_TIMEOUT_SEC = 15L

  private val coreController: CoreController =
    V2RayNativeManager.newCoreController(CoreCallback())
  private var screenReceiver: BroadcastReceiver? = null
  private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val stopLoopExecutor =
    Executors.newSingleThreadExecutor { r -> Thread(r, "V2RayStopLoop") }

  private var broadcastersHolderInternal: BroadcastersHolder? = null

  /**
   * IPC event sink for the daemon process. Set once in [Application.onCreate] before any command or
   * event can be exchanged with the main process, so a missing holder is a programming error and we
   * fail fast rather than silently dropping events.
   */
  private val broadcastersHolder: BroadcastersHolder
    get() = requireNotNull(broadcastersHolderInternal) { "broadcastersHolder not initialized" }

  fun setBroadcastersHolder(holder: BroadcastersHolder) {
    broadcastersHolderInternal = holder
  }

  @Volatile
  private var currentConfigInternal: ConnectionProfile? = null

  var serviceControl: SoftReference<ServiceControl>? = null
    set(value) {
      field = value
      V2RayNativeManager.initCoreEnv(value?.get()?.getService())
    }

  fun startVService(
    context: Context,
    guid: String? = null,
  ) {
    if (guid != null) {
      if (isRunningInternal && KeyValueStorage.getSelectServer() == guid) {
        return
      }
      KeyValueStorage.setSelectServer(guid)
    }
    if (isRunningInternal) {
      Log.i({ "startVService: core running -> restart for new profile" }, AppConfig.TAG)
      stopCoreLoop()
      startContextService(context)
      return
    }
    startContextService(context)
  }

  /**
   * Checks if the V2Ray service is running.
   * @return True if the service is running, false otherwise.
   */
  fun isRunning() = coreController.isRunning

  private val isRunningInternal get() = coreController.isRunning

  /**
   * Gets the name of the currently running server.
   * @return The name of the running server.
   */
  fun getRunningServerName() = currentConfigInternal?.remarks.orEmpty()

  fun handleDaemonCommand(
    command: FromMainToDaemon,
    control: ServiceControl?,
    context: Context,
  ) {
    when (command) {
      is FromMainToDaemon.RegisterClient -> {
        val event =
          if (isRunningInternal) {
            DaemonToMain.Running
          } else {
            DaemonToMain.NotRunning
          }
        broadcastersHolder.daemonBroadcastReceiver.sendEvent(event)
      }

      is FromMainToDaemon.UnregisterClient -> Unit // nothing to do

      is FromMainToDaemon.Start -> Unit // handled by onStartCommand

      is FromMainToDaemon.Stop -> {
        Log.i({ "Daemon command: Stop" }, AppConfig.TAG)
        control?.stopService()
      }

      is FromMainToDaemon.Restart -> {
        Log.i({ "Daemon command: Restart" }, AppConfig.TAG)
        if (control == null) {
          startContextService(context)
        } else {
          control.stopService()
          val ctx = control.getService()
          Handler(Looper.getMainLooper()).postDelayed(
            { startVService(ctx) },
            500L,
          )
        }
      }

      is FromMainToDaemon.MeasureDelay -> {
        measureV2rayDelay()
      }

      is FromMainToDaemon.SaveProfile -> {
        Log.i({ "Daemon command: Save Profile" }, AppConfig.TAG)
        broadcastersHolder.daemonBroadcastReceiver.sendEvent(DaemonToMain.SaveProfile)
      }
    }
  }

  private fun startContextService(context: Context) {
    if (isRunningInternal) {
      Log.w(
        { "startContextService skipped: core still reports running (wait for stop to finish)" },
        AppConfig.TAG,
      )
      return
    }
    val guid = KeyValueStorage.getSelectServer() ?: return
    val config = KeyValueStorage.decodeServerConfig(guid) ?: return
    if (config.protocol != Protocol.Custom &&
      config.protocol != Protocol.PolicyGroup &&
      !Utils.isValidUrl(config.server) &&
      !Utils.isPureIpAddress(config.server.orEmpty())
    ) {
      return
    }

    if (KeyValueStorage.decodeSettingsBool(AppConfig.PREF_PROXY_SHARING)) {
      Log.i(
        { context.getString(R.string.toast_warning_pref_proxysharing_short) },
        AppConfig.TAG,
      )
    } else {
      Log.i({ context.getString(R.string.toast_services_start) }, AppConfig.TAG)
    }
    val intent =
      if (SettingsManager.isVpnMode()) {
        Intent(context.applicationContext, V2RayVpnService::class.java)
      } else {
        Intent(context.applicationContext, V2RayProxyOnlyService::class.java)
      }
    ContextCompat.startForegroundService(context, intent)
  }

  /**
   * Refer to the official documentation for [registerReceiver](https://developer.android.com/reference/androidx/core/content/ContextCompat#registerReceiver(android.content.Context,android.content.BroadcastReceiver,android.content.IntentFilter,int):
   * `registerReceiver(Context, BroadcastReceiver, IntentFilter, int)`.
   * Starts the V2Ray core service.
   */
  fun startCoreLoop(vpnInterface: ParcelFileDescriptor?): Boolean {
    if (isRunningInternal) {
      return false
    }

    val service = getService() ?: return false
    val guid = KeyValueStorage.getSelectServer() ?: return false
    val config = KeyValueStorage.decodeServerConfig(guid) ?: return false

    // Set up IPC channel first so all error paths can report back
    val control =
      serviceControl?.get() ?: run {
        Log.e({ "serviceControl not available" }, AppConfig.TAG)
        return false
      }

    try {
      screenReceiver =
        object : BroadcastReceiver() {
          override fun onReceive(
            ctx: Context?,
            intent: Intent?,
          ) {
            when (intent?.action) {
              Intent.ACTION_SCREEN_OFF -> {
                Log.i({ "SCREEN_OFF, stop querying stats" }, AppConfig.TAG)
                NotificationManager.stopSpeedNotification(currentConfigInternal)
              }

              Intent.ACTION_SCREEN_ON -> {
                Log.i({ "SCREEN_ON, start querying stats" }, AppConfig.TAG)
                NotificationManager.startSpeedNotification(currentConfigInternal)
              }
            }
          }
        }
      val screenFilter = IntentFilter()
      screenFilter.addAction(Intent.ACTION_SCREEN_ON)
      screenFilter.addAction(Intent.ACTION_SCREEN_OFF)
      ContextCompat.registerReceiver(
        service,
        screenReceiver,
        screenFilter,
        Utils.receiverFlags(),
      )
    } catch (runtime: RuntimeException) {
      Log.e({ "Failed to register broadcast receiver" }, AppConfig.TAG, runtime)
      broadcastersHolder.daemonBroadcastReceiver.sendEvent(
        DaemonToMain.StartFailure(service.getString(R.string.vpn_core_receiver_register_failed)),
      )
      return false
    }

    val result =
      try {
        V2rayConfigManager.getV2rayConfig(service, guid)
      } catch (cancel: CancellationException) {
        throw cancel
      } catch (appError: AppError) {
        Log.e(
          { "Failed to get V2ray config: ${appError.message}" },
          AppConfig.TAG,
          appError,
        )
        broadcastersHolder.daemonBroadcastReceiver.sendEvent(DaemonToMain.StartFailure(appError.userReadable))
        return false
      } catch (runtime: RuntimeException) {
        Log.e({ "Failed to get V2ray config" }, AppConfig.TAG, runtime)
        val payload =
          runtime.message?.trim()?.takeIf { it.isNotEmpty() }
            ?: service.getString(R.string.vpn_core_config_build_failed)
        broadcastersHolder.daemonBroadcastReceiver.sendEvent(DaemonToMain.StartFailure(payload))
        return false
      }

    currentConfigInternal = config
    var tunFd = vpnInterface?.fd ?: 0
    if (SettingsManager.isUsingHevTun()) {
      tunFd = 0
    }

    try {
      NotificationManager.showNotification(config, false)
      CoreLogFiles.truncateAll(service)
      coreController.startLoop(result.json, tunFd)
    } catch (runtime: Exception) {
      Log.e({ "Failed to start Core loop" }, AppConfig.TAG, runtime)
      NotificationManager.cancelNotification()
      val detail = runtime.message?.trim()
      val payload =
        if (!detail.isNullOrEmpty()) {
          detail
        } else {
          service.getString(R.string.vpn_core_start_failed_generic)
        }
      broadcastersHolder.daemonBroadcastReceiver.sendEvent(DaemonToMain.StartFailure(payload))
      return false
    }

    if (!isRunningInternal) {
      broadcastersHolder.daemonBroadcastReceiver.sendEvent(
        DaemonToMain.StartFailure(service.getString(R.string.vpn_core_not_running_after_start)),
      )
      NotificationManager.cancelNotification()
      return false
    }

    try {
      broadcastersHolder.daemonBroadcastReceiver.sendEvent(DaemonToMain.StartSuccess(guid))
      NotificationManager.startSpeedNotification(currentConfigInternal)
      KeyValueStorage.setVpnSessionActive(true)
      KeyValueStorage.setVpnSessionStartEpochMs(System.currentTimeMillis())
      KeyValueStorage.setVpnSessionGuid(guid)
    } catch (runtime: RuntimeException) {
      Log.e({ "Failed to startup service" }, AppConfig.TAG, runtime)
      val detail = runtime.message?.trim()
      val payload =
        if (!detail.isNullOrEmpty()) {
          detail
        } else {
          service.getString(R.string.vpn_core_notification_failed)
        }
      broadcastersHolder.daemonBroadcastReceiver.sendEvent(DaemonToMain.StartFailure(payload))
      return false
    }
    return true
  }

  /**
   * Stops the V2Ray core service.
   * Unregisters broadcast receivers, stops notifications, and shuts down plugins.
   * @return True if the core was stopped successfully, false otherwise.
   */
  fun stopCoreLoop(): Boolean {
    val service = getService() ?: return false

    if (isRunningInternal) {
      try {
        stopLoopExecutor.submit { coreController.stopLoop() }
          .get(STOP_LOOP_TIMEOUT_SEC, TimeUnit.SECONDS)
      } catch (timeout: TimeoutException) {
        Log.e(
          { "V2Ray stopLoop timed out after ${STOP_LOOP_TIMEOUT_SEC}s" },
          AppConfig.TAG,
          timeout,
        )
      } catch (execution: ExecutionException) {
        Log.e({ "Failed to stop V2Ray loop" }, AppConfig.TAG, execution.cause ?: execution)
      } catch (interrupted: InterruptedException) {
        Thread.currentThread().interrupt()
        Log.e(
          { "Interrupted while waiting for V2Ray stopLoop" },
          AppConfig.TAG,
          interrupted,
        )
      }
    }

    currentConfigInternal = null
    KeyValueStorage.clearVpnSessionRuntime()

    broadcastersHolder.daemonBroadcastReceiver.sendEvent(DaemonToMain.StopSuccess)
    NotificationManager.cancelNotification()

    screenReceiver?.let { receiver ->
      try {
        service.unregisterReceiver(receiver)
      } catch (runtime: RuntimeException) {
        Log.e({ "Failed to unregister broadcast receiver" }, AppConfig.TAG, runtime)
      }
      screenReceiver = null
    }

    return true
  }

  fun queryStats(): List<TrafficStats> {
    // "proxy,uplink,1986;proxy,downlink,4120;" -> tag,direction,value
    val stats = coreController.queryAllOutboundTrafficStats().lowercase().split(";")
    return stats.map { raw ->
      if (raw.isBlank()) {
        TrafficStats.NotReceived
      } else {
        val parts = raw.split(",")
        val direction =
          when (parts.getOrNull(1)) {
            AppConfig.UPLINK -> TrafficStats.Direction.Up
            AppConfig.DOWNLINK -> TrafficStats.Direction.Down
            else -> TrafficStats.Direction.Empty
          }
        val bytes = parts.getOrNull(2).orEmpty()
        when (parts.getOrNull(0).orEmpty()) {
          AppConfig.TAG_PROXY -> TrafficStats.Proxy(direction, bytes)
          AppConfig.TAG_DIRECT -> TrafficStats.Direct(direction, bytes)
          else -> TrafficStats.Other(raw)
        }
      }
    }
  }

  sealed interface TrafficStats {
    data class Proxy(
      val direction: Direction,
      val bytes: String,
    ) : TrafficStats

    data class Direct(
      val direction: Direction,
      val bytes: String,
    ) : TrafficStats

    @JvmInline
    value class Other(val value: String) : TrafficStats

    data object NotReceived : TrafficStats

    enum class Direction {
      Up,
      Down,
      Empty,
    }
  }

  /**
   * Measures the connection delay for the current V2Ray configuration.
   * Tests with primary URL first, then falls back to alternative URL if needed.
   * Also fetches remote IP information if the delay test was successful.
   */
  fun measureV2rayDelay() {
    if (!isRunningInternal) {
      return
    }

    managerScope.launch {
      val service = getService() ?: return@launch
      var time = -1L
      var errorStr = ""

      try {
        time = coreController.measureDelay(SettingsManager.getDelayTestUrl())
      } catch (runtime: RuntimeException) {
        Log.e({ "Failed to measure delay with primary URL" }, AppConfig.TAG, runtime)
        errorStr = runtime.message?.substringAfter("\":") ?: "empty message"
      }
      if (time == -1L) {
        try {
          time = coreController.measureDelay(SettingsManager.getDelayTestUrl(true))
        } catch (runtime: RuntimeException) {
          Log.e(
            { "Failed to measure delay with alternative URL" },
            AppConfig.TAG,
            runtime,
          )
          errorStr = runtime.message?.substringAfter("\":") ?: "empty message"
        }
      }

      val guid = KeyValueStorage.getSelectServer()
      broadcastersHolder.daemonBroadcastReceiver.sendEvent(
        DaemonToMain.DelayMeasured(guid, time.takeIf { it >= 0 }),
      )
    }
  }

  /**
   * Gets the current service instance.
   * @return The current service instance, or null if not available.
   */
  private fun getService(): Service? {
    return serviceControl?.get()?.getService()
  }

  /**
   * Core callback handler implementation for handling V2Ray core events.
   * Handles startup, shutdown, socket protection, and status emission.
   */
  private class CoreCallback : CoreCallbackHandler {
    override fun startup(): Long {
      Log.i({ "Core callback: startup" }, AppConfig.TAG_KERNEL)
      return SUCCESS
    }

    /**
     * Called when V2Ray core shuts down.
     * @SUCCESS for success, any other value for failure.
     */
    override fun shutdown(): Long {
      Log.i({ "Core callback: shutdown" }, AppConfig.TAG_KERNEL)
      val serviceControl = serviceControl?.get() ?: return -1
      return try {
        serviceControl.stopService()
        SUCCESS
      } catch (runtime: RuntimeException) {
        Log.e({ "Failed to stop service in callback" }, AppConfig.TAG_KERNEL, runtime)
        FAILURE
      }
    }

    /**
     * Called when V2Ray core emits status information.
     * @param l Status code.
     * @param s Status message.
     * @return Always returns 0.
     */
    override fun onEmitStatus(
      l: Long,
      s: String?,
    ): Long {
      Log.i(
        { "Core callback: onEmitStatus code=$l msg=${s ?: "null"}" },
        AppConfig.TAG_KERNEL,
      )
      return SUCCESS
    }
  }
}

private const val SUCCESS = 0L
private const val FAILURE = -1L
