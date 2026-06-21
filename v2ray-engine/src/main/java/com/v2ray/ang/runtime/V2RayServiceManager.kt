package com.v2ray.ang.runtime

import android.app.Application
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.content.ContextCompat
import com.v2ray.ang.AppConfig
import com.thindie.rknzbl.v2rayengine.R
import com.v2ray.ang.enums.Protocol
import com.v2ray.ang.dto.ConnectionProfile
import com.v2ray.ang.contracts.ServiceControl
import com.v2ray.ang.error.AppError
import com.v2ray.ang.service.V2RayProxyOnlyService
import com.v2ray.ang.service.V2RayVpnService
import com.v2ray.ang.util.ConnectionProfileSummariser
import com.v2ray.ang.util.MessageUtil
import com.v2ray.ang.util.Utils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import java.lang.ref.SoftReference
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

object V2RayServiceManager {

    private const val STOP_LOOP_TIMEOUT_SEC = 15L

    private val coreController: CoreController = V2RayNativeManager.newCoreController(CoreCallback())
    private val mMsgReceive = ReceiveMessageHandler()
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var currentConfig: ConnectionProfile? = null

    var serviceControl: SoftReference<ServiceControl>? = null
        set(value) {
            field = value
            V2RayNativeManager.initCoreEnv(value?.get()?.getService())
        }

    /**
     * Starts the V2Ray service from a toggle action.
     * @param context The context from which the service is started.
     * @return True if the service was started successfully, false otherwise.
     */
    fun startVServiceFromToggle(context: Context): Boolean {
        if (KeyValueStorage.getSelectServer().isNullOrEmpty()) {
            Log.i(AppConfig.TAG, context.getString(R.string.app_tile_first_use))
            return false
        }
        startContextService(context)
        return true
    }

    /**
     * Starts the V2Ray service.
     * @param context The context from which the service is started.
     * @param guid The GUID of the server configuration to use (optional).
     */
    fun startVService(context: Context, guid: String? = null) {
        if (guid != null) {
            if (isRunningInternal && KeyValueStorage.getSelectServer() == guid) {
                return
            }
            KeyValueStorage.setSelectServer(guid)
        }
        if (isRunningInternal) {
            Log.i(AppConfig.TAG, "startVService: core running -> restart for new profile")
            MessageUtil.sendMsg2Service(context, AppConfig.MSG_STATE_RESTART, "")
            return
        }
        startContextService(context)
    }

    /**
     * Stops the V2Ray service.
     * @param context The context from which the service is stopped.
     */
    fun stopVService(context: Context) {
        MessageUtil.sendMsg2Service(context, AppConfig.MSG_STATE_STOP, "")
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
    fun getRunningServerName() = currentConfig?.remarks.orEmpty()

    /**
     * Starts the context service for V2Ray.
     * Chooses between VPN service or Proxy-only service based on user settings.
     * @param context The context from which the service is started.
     */
    private fun startContextService(context: Context) {
        if (isRunningInternal) {
            Log.w(
                AppConfig.TAG,
                "startContextService skipped: core still reports running (wait for stop to finish)",
            )
            return
        }
        val guid = KeyValueStorage.getSelectServer() ?: return
        val config = KeyValueStorage.decodeServerConfig(guid) ?: return
        if (config.protocol != Protocol.Custom
            && config.protocol != Protocol.PolicyGroup
            && !Utils.isValidUrl(config.server)
            && !Utils.isPureIpAddress(config.server.orEmpty())
        ) return


        if (KeyValueStorage.decodeSettingsBool(AppConfig.PREF_PROXY_SHARING)) {
            Log.i(AppConfig.TAG, context.getString(R.string.toast_warning_pref_proxysharing_short))
        } else {
            Log.i(AppConfig.TAG, context.getString(R.string.toast_services_start))
        }
        val intent = if (SettingsManager.isVpnMode()) {
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
    fun startCoreLoop(vpnInterface: ParcelFileDescriptor?, application: Application): Boolean {
        if (isRunningInternal) {
            return false
        }

        val service = getService() ?: return false
        val guid = KeyValueStorage.getSelectServer() ?: return false
        val config = KeyValueStorage.decodeServerConfig(guid) ?: return false
        val result = try {
            V2rayConfigManager.getV2rayConfig(service, guid)
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (appError: AppError) {
            Log.e(AppConfig.TAG, "Failed to get V2ray config: ${appError.message}", appError)
            MessageUtil.sendMsg2UI(
                service,
                AppConfig.MSG_STATE_START_FAILURE,
                appError.userReadable,
            )
            return false
        } catch (runtime: RuntimeException) {
            Log.e(AppConfig.TAG, "Failed to get V2ray config", runtime)
            val payload = runtime.message?.trim()?.takeIf { it.isNotEmpty() }
                ?: service.getString(R.string.vpn_core_config_build_failed)
            MessageUtil.sendMsg2UI(service, AppConfig.MSG_STATE_START_FAILURE, payload)
            return false
        }

        try {
            val mFilter = IntentFilter(AppConfig.BROADCAST_ACTION_SERVICE)
            mFilter.addAction(Intent.ACTION_SCREEN_ON)
            mFilter.addAction(Intent.ACTION_SCREEN_OFF)
            mFilter.addAction(Intent.ACTION_USER_PRESENT)
            ContextCompat.registerReceiver(service, mMsgReceive, mFilter, Utils.receiverFlags())
        } catch (runtime: RuntimeException) {
            Log.e(AppConfig.TAG, "Failed to register broadcast receiver", runtime)
            MessageUtil.sendMsg2UI(
                service,
                AppConfig.MSG_STATE_START_FAILURE,
                service.getString(R.string.vpn_core_receiver_register_failed),
            )
            return false
        }

        currentConfig = config
        var tunFd = vpnInterface?.fd ?: 0
        if (SettingsManager.isUsingHevTun()) {
            tunFd = 0
        }

        try {
            val isFavorite = (application as ConnectionProfileSummariser).isSavedAsFavorite(config)
            NotificationManager.showNotification(config, isFavorite)
            coreController.startLoop(result.json, tunFd)
        } catch (runtime: Exception) {
            Log.e(AppConfig.TAG, "Failed to start Core loop", runtime)
            NotificationManager.cancelNotification()
            val detail = runtime.message?.trim()
            val payload =
                if (!detail.isNullOrEmpty()) detail
                else service.getString(R.string.vpn_core_start_failed_generic)
            MessageUtil.sendMsg2UI(service, AppConfig.MSG_STATE_START_FAILURE, payload)
            return false
        }

        if (!isRunningInternal) {
            MessageUtil.sendMsg2UI(
                service,
                AppConfig.MSG_STATE_START_FAILURE,
                service.getString(R.string.vpn_core_not_running_after_start),
            )
            NotificationManager.cancelNotification()
            return false
        }

        try {
            MessageUtil.sendMsg2UI(service, AppConfig.MSG_STATE_START_SUCCESS, "")
            //NotificationManager.showNotification(currentConfig)
            NotificationManager.startSpeedNotification(currentConfig)
            KeyValueStorage.setVpnSessionActive(true)
            KeyValueStorage.setVpnSessionStartEpochMs(System.currentTimeMillis())
            KeyValueStorage.setVpnSessionGuid(guid)

        } catch (runtime: RuntimeException) {
            Log.e(AppConfig.TAG, "Failed to startup service", runtime)
            val detail = runtime.message?.trim()
            val payload =
                if (!detail.isNullOrEmpty()) detail
                else service.getString(R.string.vpn_core_notification_failed)
            MessageUtil.sendMsg2UI(service, AppConfig.MSG_STATE_START_FAILURE, payload)
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
            val done = CountDownLatch(1)
            managerScope.launch {
                try {
                    coreController.stopLoop()
                } catch (runtime: RuntimeException) {
                    Log.e(AppConfig.TAG, "Failed to stop V2Ray loop", runtime)
                } finally {
                    done.countDown()
                }
            }
            try {
                val finished = done.await(STOP_LOOP_TIMEOUT_SEC, TimeUnit.SECONDS)
                if (!finished) {
                    Log.e(AppConfig.TAG, "V2Ray stopLoop timed out after ${STOP_LOOP_TIMEOUT_SEC}s")
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                Log.e(AppConfig.TAG, "Interrupted while waiting for V2Ray stopLoop", e)
            }
        }

        currentConfig = null
        KeyValueStorage.clearVpnSessionRuntime()

        MessageUtil.sendMsg2UI(service, AppConfig.MSG_STATE_STOP_SUCCESS, "")
        NotificationManager.cancelNotification()

        try {
            service.unregisterReceiver(mMsgReceive)
        } catch (runtime: RuntimeException) {
            Log.e(AppConfig.TAG, "Failed to unregister broadcast receiver", runtime)
        }

        return true
    }

    /**
     * Queries the statistics for a given tag and link.
     * @param tag The tag to query.
     * @param link The link to query.
     * @return The statistics value.
     */
    fun queryStats(tag: String, link: String): Long {
        return coreController.queryStats(tag, link)
    }

    /**
     * Measures the connection delay for the current V2Ray configuration.
     * Tests with primary URL first, then falls back to alternative URL if needed.
     * Also fetches remote IP information if the delay test was successful.
     */
    private fun measureV2rayDelay() {
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
                Log.e(AppConfig.TAG, "Failed to measure delay with primary URL", runtime)
                errorStr = runtime.message?.substringAfter("\":") ?: "empty message"
            }
            if (time == -1L) {
                try {
                    time = coreController.measureDelay(SettingsManager.getDelayTestUrl(true))
                } catch (runtime: RuntimeException) {
                    Log.e(AppConfig.TAG, "Failed to measure delay with alternative URL", runtime)
                    errorStr = runtime.message?.substringAfter("\":") ?: "empty message"
                }
            }

            val result = if (time >= 0) {
                service.getString(R.string.connection_test_available, time)
            } else {
                service.getString(R.string.connection_test_error, errorStr)
            }
            MessageUtil.sendMsg2UI(service, AppConfig.MSG_MEASURE_DELAY_SUCCESS, result)

            // Only fetch IP info if the delay test was successful
            if (time >= 0) {
                SpeedtestManager.getRemoteIPInfo()?.let { ip ->
                    MessageUtil.sendMsg2UI(service, AppConfig.MSG_MEASURE_DELAY_SUCCESS, "$result\n$ip")
                }
            }
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
            return SUCCESS
        }

        /**
         * Called when V2Ray core shuts down.
         * @SUCCESS for success, any other value for failure.
         */
        override fun shutdown(): Long {
            val serviceControl = serviceControl?.get() ?: return -1
            return try {
                serviceControl.stopService()
                SUCCESS
            } catch (runtime: RuntimeException) {
                Log.e(AppConfig.TAG, "Failed to stop service in callback", runtime)
                FAILURE
            }
        }

        /**
         * Called when V2Ray core emits status information.
         * @param l Status code.
         * @param s Status message.
         * @return Always returns 0.
         */
        override fun onEmitStatus(l: Long, s: String?): Long {
           return SUCCESS
        }
    }

    /**
     * Broadcast receiver for handling messages sent to the service.
     * Handles registration, service control, and screen events.
     */
    private class ReceiveMessageHandler : BroadcastReceiver() {
        /**
         * Handles received broadcast messages.
         * Processes service control messages and screen state changes.
         * @param ctx The context in which the receiver is running.
         * @param intent The intent being received.
         */
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val serviceControl = serviceControl?.get() ?: return
            when (intent?.getIntExtra("key", 0)) {
                AppConfig.MSG_REGISTER_CLIENT -> {
                    if (isRunningInternal) {
                        MessageUtil.sendMsg2UI(serviceControl.getService(), AppConfig.MSG_STATE_RUNNING, "")
                    } else {
                        MessageUtil.sendMsg2UI(serviceControl.getService(), AppConfig.MSG_STATE_NOT_RUNNING, "")
                    }
                }

                AppConfig.MSG_UNREGISTER_CLIENT -> {
                    // nothing to do
                }

                AppConfig.MSG_STATE_START -> {
                    // nothing to do
                }

                AppConfig.MSG_STATE_STOP -> {
                    Log.i(AppConfig.TAG, "Stop Service")
                    serviceControl.stopService()
                }

                AppConfig.MSG_STATE_RESTART -> {
                    Log.i(AppConfig.TAG, "Restart Service")
                    serviceControl.stopService()
                    val ctx = serviceControl.getService()
                    Handler(Looper.getMainLooper()).postDelayed(
                        { startVService(ctx) },
                        500L,
                    )
                }

                AppConfig.MSG_STATE_SAVE_PROFILE -> {
                    Log.i(AppConfig.TAG, "Save Profile")
                    MessageUtil.sendMsg2UI(serviceControl.getService(), AppConfig.MSG_STATE_SAVE_PROFILE, "")
                }

                AppConfig.MSG_MEASURE_DELAY -> {
                    measureV2rayDelay()
                }
            }

            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    Log.i(AppConfig.TAG, "SCREEN_OFF, stop querying stats")
                    NotificationManager.stopSpeedNotification(currentConfig)
                }

                Intent.ACTION_SCREEN_ON -> {
                    Log.i(AppConfig.TAG, "SCREEN_ON, start querying stats")
                    NotificationManager.startSpeedNotification(currentConfig)
                }
            }
        }
    }
}

private const val SUCCESS = 0L
private const val FAILURE = -1L
