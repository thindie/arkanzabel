package com.thindie.rknzbl.application

import android.app.Application
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.thindie.engine.core.Router
import com.thindie.engine.core.WorkState
import com.thindie.rknzbl.BuildConfig
import com.thindie.rknzbl.application.di.ApplicationScope
import com.thindie.rknzbl.application.work.ActiveProfileAutoSaveWorker
import com.thindie.rknzbl.application.work.RknzblWorkerFactory
import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.ConnectionProfile
import com.v2ray.ang.runtime.KeyValueStorage
import com.v2ray.ang.runtime.SettingsManager
import com.v2ray.ang.util.ConnectionProfileSummariser
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.TimeUnit

class Application : Application(), Configuration.Provider, ConnectionProfileSummariser {
  private lateinit var applicationScopeInternal: ApplicationScope

  val applicationScope: ApplicationScope
    get() = applicationScopeInternal

  override val workManagerConfiguration: Configuration
    get() =
      Configuration.Builder()
        .setWorkerFactory(RknzblWorkerFactory(applicationScope))
        .build()

  private var router: Router? = null

  val vpnRuntimeState: StateFlow<WorkState>
    get() = applicationScope.vpnStateTracker.serviceState

  val finishCommand =
    MutableSharedFlow<Unit>(
      replay = 0,
      extraBufferCapacity = 3,
      BufferOverflow.DROP_LATEST,
    )

  override fun onCreate() {
    super.onCreate()
    AppStrings.init(this)
    AppConfig.initHostApplicationId(packageName, BuildConfig.VERSION_NAME)
    KeyValueStorage.initialize(this)
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

  override fun isSavedAsFavorite(connectionProfile: ConnectionProfile): Boolean {
    return applicationScope.connectionProfileRepository.isSaved(connectionProfile)
  }
}
