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
import com.v2ray.ang.ipc.putProfile
import com.v2ray.ang.util.JsonUtil
import com.v2ray.ang.util.Utils
import com.v2ray.ang.v2raydaemon.V2RayProxyOnlyService
import com.v2ray.ang.v2raydaemon.V2RayVpnService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import java.lang.ref.SoftReference
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

object V2RayServiceManager {
  private const val STOP_LOOP_TIMEOUT_SEC = 15L

  private val coreController: CoreController =
    V2RayNativeManager.newCoreController(CoreCallback())
  private var screenReceiver: BroadcastReceiver? = null
  private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
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

  private val currentConfigState: MutableStateFlow<ConnectionProfile?> = MutableStateFlow(null)
  var currentConfigInternal get() = currentConfigState.value
    set(value) {
      currentConfigState.update { value }
    }

  val requireCurrentProfile get() = requireNotNull(currentConfigState.value)

  var serviceControl: SoftReference<ServiceControl>? = null
    set(value) {
      field = value
      V2RayNativeManager.initCoreEnv(value?.get()?.getService())
    }

  fun startVService(
    context: Context,
    connectionProfile: ConnectionProfile,
  ) {
    if (isRunningInternal) {
      Log.i({ "startVService: core running -> restart for new profile" }, AppConfig.TAG)
      stopCoreLoop()
    }
    startContextService(context, connectionProfile)
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

      is FromMainToDaemon.Start -> {
        startVService(context, command.profile)
      }

      is FromMainToDaemon.Stop -> {
        Log.i({ "Daemon command: Stop" }, AppConfig.TAG)
        control?.stopService()
      }

      is FromMainToDaemon.Restart -> {
        Log.i({ "Daemon command: Restart" }, AppConfig.TAG)
        val profile = currentConfigInternal ?: KeyValueStorage.getLastConnectionProfile()
        if (profile == null) {
          Log.w({ "Cannot restart service: no previous profile" }, AppConfig.TAG)
          return
        }
        if (control == null) {
          startContextService(context, profile)
        } else {
          control.stopService()
          val ctx = control.getService()
          Handler(Looper.getMainLooper()).postDelayed(
            { startVService(ctx, profile) },
            500L,
          )
        }
      }

      is FromMainToDaemon.SaveProfile -> {
        Log.i({ "Daemon command: Save Profile" }, AppConfig.TAG)
        broadcastersHolder.daemonBroadcastReceiver.sendEvent(DaemonToMain.SaveProfile)
      }

      is FromMainToDaemon.MeasureProfile -> {
        Log.i({ "Daemon command: Measure Profile" }, AppConfig.TAG)
        managerScope.launch {
          val delay = measureProfileDelay(context, command.profile).getOrDefault(-1L)
          if (delay < 0) {
            Log.e({ "Failed to measure profile ${command.profile}" }, AppConfig.TAG)
          } else {
            Log.i({ "Daemon command: Measure Profile, measured: $delay" }, AppConfig.TAG)
          }
          broadcastersHolder.daemonBroadcastReceiver.sendEvent(
            DaemonToMain.ProfileMeasured(command.profile.copy(delay = delay), command.requestId),
          )
        }
      }
    }
  }

  /** Starts a temporary core for [profile] and measures outbound delay through it. */
  private fun measureProfileDelay(
    context: Context,
    profile: ConnectionProfile,
  ): Result<Long> =
    runCatching {
      val v2ray = V2rayConfigManager.getV2rayConfig4Speedtest(context, profile)
      val compactConfig = JsonUtil.parseString(v2ray.json)?.let { JsonUtil.toJson(it) } ?: v2ray.json
      val coreStarted = CountDownLatch(1)
      val ephemeral =
        V2RayNativeManager.newCoreController(
          object : CoreCallbackHandler {
            override fun onEmitStatus(
              status: Long,
              message: String?,
            ) = 0L

            override fun shutdown(): Long = 0L

            override fun startup(): Long {
              coreStarted.countDown()
              return 0L
            }
          },
        )
      val coreThread = Thread({ ephemeral.startLoop(compactConfig, 0) }, "V2RayProfilePing").apply { start() }

      try {
        check(coreStarted.await(PROFILE_PING_CORE_START_TIMEOUT_SEC, TimeUnit.SECONDS)) {
          "Timed out waiting for profile ping core startup"
        }
        val measurement =
          FutureTask {
            V2RayNativeManager.measureOutboundDelay(compactConfig, SettingsManager.getDelayTestUrl())
          }
        Thread(measurement, "V2RayProfilePingMeasure").start()
        try {
          measurement.get(PROFILE_PING_MEASURE_TIMEOUT_SEC, TimeUnit.SECONDS)
        } finally {
          measurement.cancel(true)
        }
      } finally {
        stopEphemeral(ephemeral)
        try {
          coreThread.join(PROFILE_PING_CORE_STOP_TIMEOUT_MS)
        } catch (interrupted: InterruptedException) {
          Thread.currentThread().interrupt()
        }
      }
    }

  private fun stopEphemeral(controller: CoreController) {
    try {
      controller.stopLoop()
    } catch (runtime: RuntimeException) {
      Log.e({ "Failed to stop ephemeral core loop" }, AppConfig.TAG, runtime)
    }
  }

  private fun startContextService(
    context: Context,
    connectionProfile: ConnectionProfile,
  ) {
    if (isRunningInternal) {
      Log.w(
        { "startContextService skipped: core still reports running (wait for stop to finish)" },
        AppConfig.TAG,
      )
      return
    }

    if (connectionProfile.protocol != Protocol.Custom &&
      connectionProfile.protocol != Protocol.PolicyGroup &&
      !Utils.isValidUrl(connectionProfile.server) &&
      !Utils.isPureIpAddress(connectionProfile.server.orEmpty())
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
      }.apply {
        putProfile(connectionProfile)
      }
    currentConfigInternal = connectionProfile
    KeyValueStorage.setLastConnectionProfile(connectionProfile)
    ContextCompat.startForegroundService(context, intent)
  }

  /**
   * Refer to the official documentation for [registerReceiver](https://developer.android.com/reference/androidx/core/content/ContextCompat#registerReceiver(android.content.Context,android.content.BroadcastReceiver,android.content.IntentFilter,int):
   * `registerReceiver(Context, BroadcastReceiver, IntentFilter, int)`.
   * Starts the V2Ray core service.
   */
  fun startCoreLoop(
    vpnInterface: ParcelFileDescriptor?,
    config: ConnectionProfile,
  ): Boolean {
    if (isRunningInternal) {
      return false
    }
    val service = getService() ?: return false
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
        V2rayConfigManager.getV2rayConfig(service, config)
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
      broadcastersHolder.daemonBroadcastReceiver.sendEvent(DaemonToMain.StartSuccess(requireCurrentProfile))
      NotificationManager.startSpeedNotification(currentConfigInternal)
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
private const val PROFILE_PING_CORE_START_TIMEOUT_SEC = 15L
private const val PROFILE_PING_CORE_STOP_TIMEOUT_MS = 15_000L
private const val PROFILE_PING_MEASURE_TIMEOUT_SEC = 15L
