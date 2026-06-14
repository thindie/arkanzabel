package com.thindie.rknzbl.application

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.thindie.rknzbl.BuildConfig
import com.thindie.rknzbl.R
import com.thindie.rknzbl.application.di.ApplicationScope
import com.thindie.rknzbl.application.work.ActiveProfileAutoSaveWorker
import com.thindie.rknzbl.application.work.RknzblWorkerFactory
import com.thindie.rknzbl.engine.Router
import com.thindie.rknzbl.engine.WorkState
import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.ConnectionProfile
import com.v2ray.ang.runtime.KeyValueStorage
import com.v2ray.ang.runtime.SettingsManager
import com.v2ray.ang.runtime.V2RayServiceManager
import com.v2ray.ang.util.ConnectionProfileSummariser
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

class Application : Application(), Configuration.Provider, ConnectionProfileSummariser {
  val applicationScope = ApplicationScope()
  private val appCoroutineScope =
    CoroutineScope(
      SupervisorJob() + Dispatchers.Default +
        CoroutineExceptionHandler { _, e ->
          "${e.message}"
        },
    )

  override val workManagerConfiguration: Configuration
    get() =
      Configuration.Builder()
        .setWorkerFactory(RknzblWorkerFactory(applicationScope))
        .build()

  private var router: Router? = null

  private val vpnActivityReceiver =
    object : BroadcastReceiver() {
      override fun onReceive(
        context: Context?,
        intent: Intent?,
      ) {
        if (intent?.action != AppConfig.BROADCAST_ACTION_ACTIVITY) return
        when (intent.getIntExtra("key", -1)) {
          AppConfig.MSG_STATE_START_FAILURE -> {
            Log.i(AppConfig.TAG, "vpnActivityReceiver: start == failure")
            val fallback = this@Application.getString(R.string.vpn_core_failure_unspecified)
            val broadcastString = readBroadcastString(intent, "content")
            val errorMessage = broadcastString?.trim()?.ifBlank { null } ?: fallback
            vpnRuntimeState.tryEmit(WorkState.Error(message = errorMessage))
          }

          AppConfig.MSG_STATE_RUNNING,
          AppConfig.MSG_STATE_START_SUCCESS,
          -> {
            Log.i(AppConfig.TAG, "vpnActivityReceiver: running or started")
            vpnRuntimeState.tryEmit(WorkState.Running)
          }

          AppConfig.MSG_STATE_NOT_RUNNING -> {
            Log.i(AppConfig.TAG, "vpnActivityReceiver: not running")
            vpnRuntimeState.tryEmit(WorkState.Idle)
          }
          AppConfig.MSG_STATE_STOP_SUCCESS,
          -> {
            Log.i(AppConfig.TAG, "vpnActivityReceiver: stopped")
            vpnRuntimeState.tryEmit(WorkState.Idle)
          }

          AppConfig.MSG_STATE_SAVE_PROFILE -> {
            appCoroutineScope.launch {
              Log.i(AppConfig.TAG, "vpnActivityReceiver: Save Profile: received message")
              val guid = KeyValueStorage.getSelectServer() ?: return@launch
              Log.i(AppConfig.TAG, "vpnActivityReceiver: Save Profile: selected profile determined")
              applicationScope.data.repository.save(guid)
            }
          }
        }
      }
    }

  val finishCommand =
    MutableSharedFlow<Unit>(
      replay = 0,
      extraBufferCapacity = 3,
      BufferOverflow.DROP_LATEST,
    )
  val vpnRuntimeState = MutableStateFlow<WorkState>(WorkState.Idle)

  override fun onCreate() {
    super.onCreate()
    AppStrings.init(this)
    AppConfig.initHostApplicationId(packageName, BuildConfig.VERSION_NAME)
    KeyValueStorage.initialize(this)
    SettingsManager.ensureDefaultSettings()
    SettingsManager.initRoutingRulesets(this)
    SettingsManager.initAssets(this, assets)
    SettingsManager.migrateHysteria2PinSHA256()
    ContextCompat.registerReceiver(
      this,
      vpnActivityReceiver,
      IntentFilter(AppConfig.BROADCAST_ACTION_ACTIVITY),
      ContextCompat.RECEIVER_NOT_EXPORTED,
    )
    enqueueActiveProfileAutoSaveWork()
    vpnRuntimeState.value = if (V2RayServiceManager.isRunning()) WorkState.Running else WorkState.Idle
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

  private fun readBroadcastString(
    intent: Intent,
    key: String,
  ): String? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      intent.getSerializableExtra(key, String::class.java)
    } else {
      @Suppress("DEPRECATION")
      intent.getSerializableExtra(key)
        as? String
    }

  override fun isSavedAsFavorite(connectionProfile: ConnectionProfile): Boolean {
    return runBlocking { applicationScope.data.repository.isSaved(connectionProfile) }
  }
}
