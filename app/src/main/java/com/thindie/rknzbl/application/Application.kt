package com.thindie.rknzbl.application

import android.app.Application
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.thindie.engine.core.Log
import com.thindie.engine.core.Router
import com.thindie.engine.core.WorkState
import com.thindie.rknzbl.BuildConfig
import com.thindie.rknzbl.appfeatures.home.data.V2RayVpnServiceGateway
import com.thindie.rknzbl.application.di.ApplicationScope
import com.thindie.rknzbl.application.work.ActiveProfileAutoSaveWorker
import com.thindie.rknzbl.application.work.RknzblWorkerFactory
import com.v2ray.ang.AppConfig
import com.v2ray.ang.ipc.BroadcastersHolder
import com.v2ray.ang.ipc.DaemonToMain
import com.v2ray.ang.ipc.IpcDaemonBroadcastReceiver
import com.v2ray.ang.ipc.IpcMainBroadcastReceiver
import com.v2ray.ang.runtime.KeyValueStorage
import com.v2ray.ang.runtime.SettingsManager
import com.v2ray.ang.runtime.V2RayServiceManager
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class Application : Application(), Configuration.Provider, BroadcastersHolder {
  override val daemonBroadcastReceiver: IpcDaemonBroadcastReceiver by lazy {
    IpcDaemonBroadcastReceiver(this)
  }

  override val mainBroadcastReceiver: IpcMainBroadcastReceiver by lazy {
    IpcMainBroadcastReceiver(this)
  }

  private lateinit var applicationScopeInternal: ApplicationScope

  val applicationScope: ApplicationScope
    get() = applicationScopeInternal

  override val workManagerConfiguration: Configuration
    get() =
      Configuration.Builder()
        .setWorkerFactory(RknzblWorkerFactory(applicationScope))
        .build()

  private var router: Router? = null

  val finishCommand =
    MutableSharedFlow<Unit>(
      replay = 0,
      extraBufferCapacity = 3,
      BufferOverflow.DROP_LATEST,
    )

  override fun onCreate() {
    super.onCreate()
    KeyValueStorage.initialize(this)
    AppStrings.init(this)
    AppConfig.initHostApplicationId(packageName, BuildConfig.VERSION_NAME)
    applicationScopeInternal = ApplicationScope.configure(this)
    SettingsManager.ensureDefaultSettings()
    SettingsManager.initRoutingRulesets(this)
    SettingsManager.initAssets(this, assets)
    SettingsManager.migrateHysteria2PinSHA256()
    collectDaemonProcessEvents(applicationScope.vpnStateTracker as V2RayVpnServiceGateway)
    mainBroadcastReceiver.startObserving()
    enqueueActiveProfileAutoSaveWork()
    V2RayServiceManager.setBroadcastersHolder(this)
    daemonBroadcastReceiver.startObserving()
  }

  private fun enqueueActiveProfileAutoSaveWork() {
    val request =
      PeriodicWorkRequestBuilder<ActiveProfileAutoSaveWorker>(
        repeatInterval = 15,
        repeatIntervalTimeUnit = TimeUnit.MINUTES,
      )
        .build()
    WorkManager.getInstance(this).enqueueUniquePeriodicWork(
      AppConfig.ACTIVE_PROFILE_AUTO_SAVE_WORK_NAME,
      ExistingPeriodicWorkPolicy.KEEP,
      request,
    )
  }

  fun requireRouter(): Router {
    if (router == null) {
      router =
        Router {
          finishCommand.tryEmit(Unit)
          router = null
        }
    }
    return requireNotNull(router)
  }

  private fun collectDaemonProcessEvents(gateway: V2RayVpnServiceGateway) {
    applicationScope.coroutineScope.launch {
      IpcMainBroadcastReceiver.events.collect { event ->
        when (event) {
          is DaemonToMain.StartFailure -> {
            Log.w({ "ipc: start == failure" }, "VPN")
            gateway.serviceState.value = WorkState.Error(message = event.message)
          }

          is DaemonToMain.Running,
          is DaemonToMain.StartSuccess,
          -> {
            Log.i({ "ipc: running" }, "VPN")
            gateway.serviceState.value = WorkState.Running
          }

          is DaemonToMain.NotRunning,
          is DaemonToMain.StopSuccess,
          -> {
            Log.i({ "ipc: stopped" }, "VPN")
            gateway.serviceState.value = WorkState.Idle
          }

          is DaemonToMain.DelayMeasured -> {
            Log.i({ "ipc: delay measured ${event.guid}=${event.delayMs}" }, "VPN")
          }

          is DaemonToMain.ConfigTestResult,
          is DaemonToMain.ConfigTestProgress,
          is DaemonToMain.ConfigTestFinished,
          -> {
            Log.i({ "ipc: config test event" }, "VPN")
          }

          is DaemonToMain.SaveProfile -> {
            Log.d({ "ipc: Save Profile requested" }, "VPN")
            val guid = KeyValueStorage.getSelectServer()
            if (guid != null) applicationScope.connectionProfileRepository.save(guid)
          }
        }
      }
    }
  }
}
