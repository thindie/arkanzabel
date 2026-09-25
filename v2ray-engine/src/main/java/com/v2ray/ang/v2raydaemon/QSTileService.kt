package com.v2ray.ang.v2raydaemon

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.thindie.engine.core.Log
import com.thindie.rknzbl.v2rayengine.R
import com.v2ray.ang.AppConfig
import com.v2ray.ang.ipc.BroadcastersHolder
import com.v2ray.ang.ipc.DaemonToMain
import com.v2ray.ang.ipc.FromMainToDaemon.Start
import com.v2ray.ang.ipc.FromMainToDaemon.Stop
import com.v2ray.ang.ipc.IpcMainBroadcastReceiver
import com.v2ray.ang.runtime.V2RayServiceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class QSTileService : TileService() {
  private val holder get() = applicationContext as BroadcastersHolder
  private var job: kotlinx.coroutines.Job? = null

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

    holder.mainBroadcastReceiver.startObserving()
    job =
      CoroutineScope(Dispatchers.Main).launch {
        IpcMainBroadcastReceiver.events.collect { event ->
          when (event) {
            is DaemonToMain.Running,
            is DaemonToMain.StartSuccess,
            -> setState(Tile.STATE_ACTIVE)

            is DaemonToMain.NotRunning,
            is DaemonToMain.StopSuccess,
            is DaemonToMain.StartFailure,
            -> setState(Tile.STATE_INACTIVE)

            else -> {}
          }
        }
      }
  }

  override fun onStopListening() {
    super.onStopListening()
    job?.cancel()
    try {
      holder.mainBroadcastReceiver.stopObserving()
    } catch (e: IllegalArgumentException) {
      Log.w({ "QS tile receiver not registered" }, AppConfig.TAG, e)
    }
  }

  override fun onClick() {
    super.onClick()
    when (qsTile?.state) {
      Tile.STATE_INACTIVE -> holder.mainBroadcastReceiver.send(Start(null))
      Tile.STATE_ACTIVE -> holder.mainBroadcastReceiver.send(Stop)
      else -> {}
    }
  }
}
