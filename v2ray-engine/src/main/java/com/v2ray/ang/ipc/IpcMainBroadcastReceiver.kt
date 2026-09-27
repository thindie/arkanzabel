package com.v2ray.ang.ipc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.thindie.engine.core.Log
import com.thindie.engine.core.ProcessKind
import com.thindie.engine.core.determineProcess
import com.v2ray.ang.AppConfig
import com.v2ray.ang.util.Utils
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class IpcMainBroadcastReceiver(private val context: Context) {
  companion object {
    private val sharedEventInternal =
      MutableSharedFlow<DaemonToMain>(
        replay = 0,
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
      )

    val events: Flow<DaemonToMain> = sharedEventInternal.asSharedFlow()
  }

  private var receiverRegistered = false

  private val receiver =
    object : BroadcastReceiver() {
      override fun onReceive(
        ctx: Context?,
        intent: Intent?,
      ) {
        if (ctx?.determineProcess() != ProcessKind.Main) return
        if (intent?.action != AppConfig.BROADCAST_ACTION_ACTIVITY) return
        val event = intent.getCoreExtra()
        Log.i({ "MainBroadcast received: $event" }, AppConfig.TAG)
        event ?: return
        sharedEventInternal.tryEmit(event)
      }
    }

  fun send(command: FromMainToDaemon) {
    try {
      val intent =
        Intent().apply {
          action = AppConfig.BROADCAST_ACTION_SERVICE
          `package` = AppConfig.ANG_PACKAGE
          putCoreExtra(command)
        }
      context.sendBroadcast(intent)
      Log.i({ "MainBroadcast sent to Daemon: $command" }, AppConfig.TAG)
    } catch (e: Exception) {
      Log.e({ "MainBroadcast, failed to send command: $command" }, AppConfig.TAG, e)
    }
  }

  fun startObserving() {
    if (receiverRegistered) return
    try {
      context.registerReceiver(
        receiver,
        IntentFilter(AppConfig.BROADCAST_ACTION_ACTIVITY),
        Utils.receiverFlags(),
      )
      receiverRegistered = true
    } catch (e: Exception) {
      Log.e({ "Failed to register daemon event receiver" }, AppConfig.TAG, e)
    }
  }

  fun stopObserving() {
    if (!receiverRegistered) return
    try {
      context.unregisterReceiver(receiver)
      receiverRegistered = false
    } catch (e: Exception) {
      Log.e({ "Failed to unregister daemon event receiver" }, AppConfig.TAG, e)
    }
  }
}
