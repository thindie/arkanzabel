package com.v2ray.ang.v2raydaemon

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.v2ray.ang.ipc.BroadcastersHolder
import com.v2ray.ang.ipc.FromMainToDaemon.Start
import com.v2ray.ang.runtime.KeyValueStorage

class BootReceiver : BroadcastReceiver() {
  /**
   * This method is called when the BroadcastReceiver is receiving an Intent broadcast.
   * It checks if the context is not null and the action is ACTION_BOOT_COMPLETED.
   * If the conditions are met, it starts the V2Ray service.
   *
   * @param context The Context in which the receiver is running.
   * @param intent The Intent being received.
   */
  override fun onReceive(
    context: Context?,
    intent: Intent?,
  ) {
    if (context == null || intent?.action != Intent.ACTION_BOOT_COMPLETED) return
    if (!KeyValueStorage.decodeStartOnBoot() ||
      KeyValueStorage.getSelectServer()
        .isNullOrEmpty()
    ) {
      return
    }
    val guid = KeyValueStorage.getSelectServer()
    (context.applicationContext as BroadcastersHolder).mainBroadcastReceiver.send(Start(guid))
  }
}
