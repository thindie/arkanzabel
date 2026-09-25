package com.v2ray.ang.ipc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.thindie.engine.core.Log
import com.v2ray.ang.AppConfig
import com.v2ray.ang.runtime.V2RayServiceManager

class IpcDaemonBroadcastReceiver(private val context: Context) {
  private var receiverRegistered = false

  private val receiver =
    object : BroadcastReceiver() {
      override fun onReceive(
        ctx: Context?,
        intent: Intent?,
      ) {
        if (intent?.action != AppConfig.BROADCAST_ACTION_SERVICE) return
        val command = handleIntent(intent) ?: return
        Log.i({ "Daemon received command: $command" }, AppConfig.TAG_KERNEL)
        dispatchCommand(command)
      }

      private fun dispatchCommand(command: FromMainToDaemon) {
        val control = V2RayServiceManager.serviceControl?.get()
        when (command) {
          is FromMainToDaemon.RegisterClient ->
            V2RayServiceManager.handleDaemonCommand(command, control, context)

          is FromMainToDaemon.Start ->
            V2RayServiceManager.startVService(context, command.guid)

          is FromMainToDaemon.Stop ->
            V2RayServiceManager.stopCoreLoop()

          is FromMainToDaemon.Restart ->
            V2RayServiceManager.handleDaemonCommand(command, control, context)

          else -> Unit
        }
      }
    }

  fun sendEvent(event: DaemonToMain) {
    try {
      val intent = handleDaemonEvent(event) ?: return
      context.sendBroadcast(intent)
      Log.i({ "Daemon sent event: $event" }, AppConfig.TAG_KERNEL)
    } catch (e: Exception) {
      Log.e({ "Failed to send daemon event: $event" }, AppConfig.TAG_KERNEL, e)
    }
  }

  fun startObserving() {
    if (receiverRegistered) return
    try {
      context.registerReceiver(receiver, IntentFilter(AppConfig.BROADCAST_ACTION_SERVICE))
      receiverRegistered = true
    } catch (e: Exception) {
      Log.e({ "Failed to register daemon command receiver" }, AppConfig.TAG_KERNEL, e)
    }
  }

  fun stopObserving() {
    if (!receiverRegistered) return
    try {
      context.unregisterReceiver(receiver)
      receiverRegistered = false
    } catch (e: Exception) {
      Log.e({ "Failed to unregister daemon command receiver" }, AppConfig.TAG_KERNEL, e)
    }
  }
}
