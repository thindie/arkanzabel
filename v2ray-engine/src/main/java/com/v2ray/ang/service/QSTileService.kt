package com.v2ray.ang.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import androidx.core.content.ContextCompat
import com.thindie.rknzbl.v2rayengine.R
import com.v2ray.ang.AppConfig
import com.v2ray.ang.runtime.V2RayServiceManager
import com.v2ray.ang.util.MessageUtil
import com.v2ray.ang.util.Utils

class QSTileService : TileService() {

  private val tileStateReceiver =
    object : BroadcastReceiver() {
      override fun onReceive(ctx: Context?, intent: Intent?) {
        when (intent?.getIntExtra("key", 0)) {
          AppConfig.MSG_STATE_RUNNING -> setState(Tile.STATE_ACTIVE)
          AppConfig.MSG_STATE_NOT_RUNNING -> setState(Tile.STATE_INACTIVE)
          AppConfig.MSG_STATE_START_SUCCESS -> setState(Tile.STATE_ACTIVE)
          AppConfig.MSG_STATE_START_FAILURE -> setState(Tile.STATE_INACTIVE)
          AppConfig.MSG_STATE_STOP_SUCCESS -> setState(Tile.STATE_INACTIVE)
        }
      }
    }

  fun setState(state: Int) {
    qsTile?.icon = Icon.createWithResource(applicationContext, R.drawable.ic_stat_name)
    if (state == Tile.STATE_INACTIVE) {
      qsTile?.state = Tile.STATE_INACTIVE
      qsTile?.label = getString(R.string.app_tile_name)
    } else if (state == Tile.STATE_ACTIVE) {
      qsTile?.state = Tile.STATE_ACTIVE
      qsTile?.label = V2RayServiceManager.getRunningServerName()
    }
    qsTile?.updateTile()
  }

  override fun onStartListening() {
    super.onStartListening()
    if (V2RayServiceManager.isRunning()) {
      setState(Tile.STATE_ACTIVE)
    } else {
      setState(Tile.STATE_INACTIVE)
    }
    try {
      applicationContext.unregisterReceiver(tileStateReceiver)
    } catch (_: IllegalArgumentException) {
      // not registered yet
    }
    val filter = IntentFilter(AppConfig.BROADCAST_ACTION_ACTIVITY)
    ContextCompat.registerReceiver(
      applicationContext,
      tileStateReceiver,
      filter,
      Utils.receiverFlags(),
    )
    MessageUtil.sendMsg2Service(this, AppConfig.MSG_REGISTER_CLIENT, "")
  }

  override fun onStopListening() {
    super.onStopListening()
    try {
      applicationContext.unregisterReceiver(tileStateReceiver)
    } catch (e: IllegalArgumentException) {
      Log.w(AppConfig.TAG, "QS tile receiver not registered", e)
    }
  }

  override fun onClick() {
    super.onClick()
    when (qsTile?.state) {
      Tile.STATE_INACTIVE -> V2RayServiceManager.startVServiceFromToggle(this)
      Tile.STATE_ACTIVE -> V2RayServiceManager.stopVService(this)
      else -> {}
    }
  }
}
