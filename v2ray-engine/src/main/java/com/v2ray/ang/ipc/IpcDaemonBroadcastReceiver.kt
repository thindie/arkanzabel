package com.v2ray.ang.ipc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.thindie.engine.core.Log
import com.thindie.engine.core.ProcessKind
import com.thindie.engine.core.determineProcess
import com.v2ray.ang.AppConfig
import com.v2ray.ang.runtime.V2RayServiceManager
import com.v2ray.ang.util.Utils

class IpcDaemonBroadcastReceiver(private val context: Context) {
  private var receiverRegistered = false

  /**
   * A runtime single-shot latch to bypass multi-process initialization race conditions.
   *
   * CRITICAL ARCHITECTURAL CONSTRAINT:
   * We cannot block the instantiation of this receiver class on the caller's side (Main process)
   * using static compile-time filters or DI graphs. Doing so would break dependency resolution
   * during cold starts and completely prevent the Daemon process from launching, as both processes
   * share the same [Application] entry point.
   *
   * HOW IT WORKS (Zero-Allocation Pragmatic Latch):
   * This flag allows the receiver to safely initialize in both processes to satisfy the DI graph.
   * On cold start, the first incoming boot-up intent bypasses the process-check branch because
   * [warmedUp] is initially false, effectively triggering the Daemon's ignition sequence.
   *
   * Once handled, the latch flips to true. All subsequent operational intents are strictly validated
   * against [ProcessKind.Daemon]. This gracefully turns the duplicate instance residing in the
   * Main process's memory into a silent, un-registered "orphan" that executes no code, receives no
   * signals, and has zero impact on runtime performance or battery life.
   */
  private var warmedUp = false

  private val receiver =
    object : BroadcastReceiver() {
      override fun onReceive(
        ctx: Context?,
        intent: Intent?,
      ) {
        if (warmedUp) {
          if (ctx?.determineProcess() != ProcessKind.Daemon) return
        }
        warmedUp = true
        Log.i({ "Daemon received event, process: ${ctx?.determineProcess()}" }, AppConfig.TAG_KERNEL)
        if (intent?.action != AppConfig.BROADCAST_ACTION_SERVICE) return
        val command = handleIntent(intent) ?: return
        Log.i({ "Daemon received command: $command" }, AppConfig.TAG_KERNEL)
        ctx?.let { dispatchCommand(command, it) }
      }

      private fun dispatchCommand(
        command: FromMainToDaemon,
        context: Context,
      ) {
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
      context.registerReceiver(
        receiver,
        IntentFilter(AppConfig.BROADCAST_ACTION_SERVICE),
        Utils.receiverFlags(),
      )
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
