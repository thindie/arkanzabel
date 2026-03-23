package com.thindie.rknzbl.application

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.content.ContextCompat
import com.thindie.rknzbl.BuildConfig
import com.thindie.rknzbl.R
import com.thindie.rknzbl.engine.Router
import com.thindie.rknzbl.engine.WorkState
import com.v2ray.ang.AppConfig
import com.v2ray.ang.handler.KeyValueStorage
import com.v2ray.ang.handler.SettingsManager
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

class Application : Application() {
  private var router: Router? = null

  private val vpnActivityReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
      if (intent?.action != AppConfig.BROADCAST_ACTION_ACTIVITY) return
      when (intent.getIntExtra("key", -1)) {
        AppConfig.MSG_STATE_START_FAILURE -> {
          val fallback = context?.getString(R.string.vpn_core_failure_unspecified)
            ?: FALLBACK_VPN_FAILURE_MESSAGE
          val broadcastString = readBroadcastString(intent, "content")
          val errorMessage = broadcastString?.trim()?.ifBlank { null } ?: fallback
          vpnRuntimeState.value = WorkState.Error(message = errorMessage)
        }

        AppConfig.MSG_STATE_RUNNING,
        AppConfig.MSG_STATE_START_SUCCESS,
          -> {
          vpnRuntimeState.value = WorkState.Running
        }

        AppConfig.MSG_STATE_NOT_RUNNING,
        AppConfig.MSG_STATE_STOP_SUCCESS,
          -> {
          vpnRuntimeState.value = WorkState.Idle
        }
      }
    }
  }

  val finishCommand = MutableSharedFlow<Unit>(
    replay = 0, extraBufferCapacity = 3, BufferOverflow.DROP_LATEST
  )
  val vpnRuntimeState = MutableStateFlow<WorkState>(WorkState.Idle)

  override fun onCreate() {
    super.onCreate()
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
  }


  fun requireRouter(): Router {
    if (router == null) {
      router = Router {
        finishCommand.tryEmit(Unit)
        router = null
      }
    }
    return requireNotNull(router)
  }

  private fun readBroadcastString(intent: Intent, key: String): String? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      intent.getSerializableExtra(key, String::class.java)
    } else {
      @Suppress("DEPRECATION") intent.getSerializableExtra(key) as? String
    }
}

private const val FALLBACK_VPN_FAILURE_MESSAGE = "VPN core start failure."