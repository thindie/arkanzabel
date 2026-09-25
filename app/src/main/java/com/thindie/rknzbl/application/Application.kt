package com.thindie.rknzbl.application

import android.app.ActivityManager
import android.app.Application
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.thindie.engine.core.Router
import com.thindie.rknzbl.BuildConfig
import com.thindie.rknzbl.application.di.ApplicationScope
import com.thindie.rknzbl.application.work.ActiveProfileAutoSaveWorker
import com.thindie.rknzbl.application.work.RknzblWorkerFactory
import com.v2ray.ang.AppConfig
import com.v2ray.ang.ipc.BroadcastersHolder
import com.v2ray.ang.ipc.IpcDaemonBroadcastReceiver
import com.v2ray.ang.ipc.IpcMainBroadcastReceiver
import com.v2ray.ang.runtime.KeyValueStorage
import com.v2ray.ang.runtime.SettingsManager
import com.v2ray.ang.runtime.V2RayServiceManager
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import java.util.concurrent.TimeUnit

class Application : Application(), Configuration.Provider, BroadcastersHolder {
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

    // Common initialization for all processes
    AppStrings.init(this)
    AppConfig.initHostApplicationId(packageName, BuildConfig.VERSION_NAME)
    KeyValueStorage.initialize(this)

    // Daemon process only needs minimal setup — no UI, no repositories,
    // no WorkManager. It runs V2Ray services directly via V2RayServiceManager.
    val isDaemonProcess =
      getSystemService(ActivityManager::class.java).runningAppProcesses
        ?.firstOrNull { it.pid == android.os.Process.myPid() }
        ?.processName
        ?.endsWith(":RunSoLibV2RayDaemon") == true
    if (isDaemonProcess) {
      // Daemon process: set up IPC receivers for command/event exchange with main process
      V2RayServiceManager.setBroadcastersHolder(this)
      daemonBroadcastReceiver.startObserving()
      mainBroadcastReceiver.startObserving()
      return
    }

    // Main process: full dependency graph and background work
    applicationScopeInternal = ApplicationScope.configure(this)
    SettingsManager.ensureDefaultSettings()
    SettingsManager.initRoutingRulesets(this)
    SettingsManager.initAssets(this, assets)
    SettingsManager.migrateHysteria2PinSHA256()
    enqueueActiveProfileAutoSaveWork()
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

  override val daemonBroadcastReceiver: IpcDaemonBroadcastReceiver by lazy {
    IpcDaemonBroadcastReceiver(this)
  }

  override val mainBroadcastReceiver: IpcMainBroadcastReceiver by lazy {
    IpcMainBroadcastReceiver(this)
  }
}
