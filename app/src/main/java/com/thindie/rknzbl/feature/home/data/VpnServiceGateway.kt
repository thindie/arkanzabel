package com.thindie.rknzbl.feature.home.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.content.ContextCompat
import com.thindie.engine.core.Log
import com.thindie.engine.core.WorkState
import com.thindie.rknzbl.R
import com.thindie.rknzbl.application.Application
import com.v2ray.ang.AppConfig
import com.v2ray.ang.runtime.KeyValueStorage
import com.v2ray.ang.runtime.V2RayServiceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Thin wrapper around [V2RayServiceManager] so [ConnectionProfileRepositoryImpl] can be
 * unit-tested without initializing the native V2Ray core (the manager's static init loads JNI).
 */
interface VpnServiceGateway {
  fun startVService(guid: String)

  fun stopVService()

  fun isRunning(): Boolean

  fun getRunningServerName(): String

  val serviceState: StateFlow<WorkState>
}

internal class V2RayVpnServiceGateway private constructor(
  private val application: Application,
) : VpnServiceGateway {
  private val scope get() = application.applicationScope.coroutineScope

  override val serviceState = MutableStateFlow<WorkState>(if (V2RayServiceManager.isRunning()) WorkState.Running else WorkState.Idle)

  private val vpnActivityReceiver =
    object : BroadcastReceiver() {
      override fun onReceive(
        context: Context?,
        intent: Intent?,
      ) {
        if (intent?.action != AppConfig.BROADCAST_ACTION_ACTIVITY) return
        when (intent.getIntExtra("key", -1)) {
          AppConfig.MSG_STATE_START_FAILURE -> {
            Log.w({ "vpnActivityReceiver: start == failure" }, AppConfig.TAG)
            val fallback = application.getString(R.string.vpn_core_failure_unspecified)
            val broadcastString = readBroadcastString(intent)
            val errorMessage = broadcastString?.trim()?.ifBlank { null } ?: fallback
            serviceState.value = WorkState.Error(message = errorMessage)
          }

          AppConfig.MSG_STATE_RUNNING -> {
            Log.i({ "vpnActivityReceiver: running" }, AppConfig.TAG)
            serviceState.value = WorkState.Running
          }
          AppConfig.MSG_STATE_START_SUCCESS -> {
            Log.i({ "vpnActivityReceiver: started" }, AppConfig.TAG)
            serviceState.value = WorkState.Running
          }

          AppConfig.MSG_STATE_NOT_RUNNING -> {
            Log.i({ "vpnActivityReceiver: not running" }, AppConfig.TAG)
            serviceState.value = WorkState.Idle
          }
          AppConfig.MSG_STATE_STOP_SUCCESS,
          -> {
            Log.i({ "vpnActivityReceiver: stopped" }, AppConfig.TAG)
            serviceState.value = WorkState.Idle
          }

          AppConfig.MSG_STATE_SAVE_PROFILE -> {
            scope.launch {
              Log.d({ "vpnActivityReceiver: Save Profile: received message" }, AppConfig.TAG)
              val guid = KeyValueStorage.getSelectServer() ?: return@launch
              Log.d({ "vpnActivityReceiver: Save Profile: selected profile determined" }, AppConfig.TAG)
              application.applicationScope.connectionProfileRepository.save(guid)
            }
          }
        }
      }
    }

  private fun readBroadcastString(
    intent: Intent,
    key: String = "content",
  ): String? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      intent.getSerializableExtra(key, String::class.java)
    } else {
      @Suppress("DEPRECATION")
      intent.getSerializableExtra(key)
        as? String
    }
  }

  override fun startVService(guid: String) = V2RayServiceManager.startVService(application, guid)

  override fun stopVService() = V2RayServiceManager.stopVService(application)

  override fun isRunning(): Boolean = serviceState.value is WorkState.Running

  override fun getRunningServerName(): String = V2RayServiceManager.getRunningServerName()

  companion object {
    fun instance(application: Application): VpnServiceGateway {
      return V2RayVpnServiceGateway(application).apply {
        ContextCompat.registerReceiver(
          application,
          vpnActivityReceiver,
          IntentFilter(AppConfig.BROADCAST_ACTION_ACTIVITY),
          ContextCompat.RECEIVER_NOT_EXPORTED,
        )
      }
    }
  }
}
