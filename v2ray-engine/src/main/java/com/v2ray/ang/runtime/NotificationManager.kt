package com.v2ray.ang.runtime

import android.app.Notification
import android.app.NotificationChannel
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.thindie.rknzbl.v2rayengine.R
import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.ConnectionProfile
import com.v2ray.ang.extension.toSpeedString
import com.v2ray.ang.runtime.V2RayServiceManager.TrafficStats
import java.lang.ref.WeakReference
import kotlin.math.min

private typealias SysNotificationManager = android.app.NotificationManager

object NotificationManager {
  private const val NOTIFICATION_ID = 1
  private const val NOTIFICATION_PENDING_INTENT_CONTENT = 0
  private const val NOTIFICATION_PENDING_INTENT_STOP_V2RAY = 1
  private const val NOTIFICATION_PENDING_INTENT_RESTART_V2RAY = 2

  private const val NOTIFICATION_PENDING_INTENT_SAVE_PROFILE = 3
  private const val NOTIFICATION_ICON_THRESHOLD = 3000

  private val speedHandler = Handler(Looper.getMainLooper())
  private var speedTickRunnable: Runnable? = null
  private var lastQueryTime = 0L
  private var _notificationCompatBuilder: WeakReference<NotificationCompat.Builder>? = null
  private val notificationCompatBuilder get() = _notificationCompatBuilder?.get()
  private var mNotificationManager: SysNotificationManager? = null

  fun startSpeedNotification(connectionProfile: ConnectionProfile?) {
    if (!KeyValueStorage.decodeSettingsBool(AppConfig.PREF_SPEED_ENABLED)) return
    if (speedTickRunnable != null || !V2RayServiceManager.isRunning()) return

    lastQueryTime = System.currentTimeMillis()
    var lastZeroSpeed = false
    val outboundTags =
      connectionProfile?.getAllOutboundTags()?.filterNot { it == AppConfig.TAG_DIRECT } ?: emptyList()

    val tick =
      object : Runnable {
        override fun run() {
          if (speedTickRunnable != this) return
          if (!V2RayServiceManager.isRunning()) {
            speedTickRunnable = null
            return
          }
          val queryTime = System.currentTimeMillis()
          val sinceLastQueryInSeconds = (queryTime - lastQueryTime) / 1000.0
          val stats = V2RayServiceManager.queryStats()
          val proxyTotal =
            outboundTags.sumOf { tag ->
              trafficBytes(stats, tag, TrafficStats.Direction.Up) +
                trafficBytes(stats, tag, TrafficStats.Direction.Down)
            }
          val directUplink = trafficBytes(stats, AppConfig.TAG_DIRECT, TrafficStats.Direction.Up)
          val directDownlink = trafficBytes(stats, AppConfig.TAG_DIRECT, TrafficStats.Direction.Down)
          val zeroSpeed = proxyTotal == 0L && directUplink == 0L && directDownlink == 0L
          if (!zeroSpeed || !lastZeroSpeed) {
            val text = StringBuilder()
            if (proxyTotal == 0L) {
              outboundTags.firstOrNull()?.let { appendSpeedString(text, it, 0.0, 0.0) }
            }
            outboundTags.forEach { tag ->
              val up = trafficBytes(stats, tag, TrafficStats.Direction.Up)
              val down = trafficBytes(stats, tag, TrafficStats.Direction.Down)
              if (up + down > 0) {
                appendSpeedString(
                  text,
                  tag,
                  up / sinceLastQueryInSeconds,
                  down / sinceLastQueryInSeconds,
                )
              }
            }
            appendSpeedString(
              text,
              AppConfig.TAG_DIRECT,
              directUplink / sinceLastQueryInSeconds,
              directDownlink / sinceLastQueryInSeconds,
            )
            updateNotification(text.toString(), proxyTotal, directDownlink + directUplink)
          }
          lastZeroSpeed = zeroSpeed
          lastQueryTime = queryTime
          speedHandler.postDelayed(this, 3000L)
        }
      }
    speedTickRunnable = tick
    speedHandler.post(tick)
  }

  fun showNotification(
    connectionProfile: ConnectionProfile?,
    hideSaveOption: Boolean,
  ) {
    val service = getService() ?: return
    val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

    val main = ComponentName(AppConfig.ANG_PACKAGE, "${AppConfig.ANG_PACKAGE}.MainActivity")
    val startMainIntent = Intent().setComponent(main)
    val contentPendingIntent =
      PendingIntent.getActivity(service, NOTIFICATION_PENDING_INTENT_CONTENT, startMainIntent, flags)

    val stopV2RayIntent =
      Intent(AppConfig.BROADCAST_ACTION_SERVICE)
        .apply {
          `package` = AppConfig.ANG_PACKAGE
          putExtra("key", AppConfig.MSG_STATE_STOP)
        }

    val stopV2RayPendingIntent =
      PendingIntent.getBroadcast(
        service,
        NOTIFICATION_PENDING_INTENT_STOP_V2RAY,
        stopV2RayIntent,
        flags,
      )

    val restartV2RayIntent =
      Intent(AppConfig.BROADCAST_ACTION_SERVICE)
        .apply {
          `package` = AppConfig.ANG_PACKAGE
          putExtra("key", AppConfig.MSG_STATE_RESTART)
        }

    val restartV2RayPendingIntent =
      PendingIntent.getBroadcast(
        service,
        NOTIFICATION_PENDING_INTENT_RESTART_V2RAY,
        restartV2RayIntent,
        flags,
      )

    val saveProfileIntent =
      Intent(AppConfig.BROADCAST_ACTION_SERVICE)
        .apply {
          `package` = AppConfig.ANG_PACKAGE
          putExtra("key", AppConfig.MSG_STATE_SAVE_PROFILE)
        }

    val saveProfilePendingIntent =
      PendingIntent.getBroadcast(
        service,
        NOTIFICATION_PENDING_INTENT_SAVE_PROFILE,
        saveProfileIntent,
        flags,
      )

    val channelId =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        createNotificationChannel()
      } else {
        ""
      }

    _notificationCompatBuilder =
      WeakReference(
        NotificationCompat.Builder(service, channelId)
          .setSmallIcon(R.drawable.ic_rknzbl)
          .setContentTitle(connectionProfile?.remarks)
          .setPriority(NotificationCompat.PRIORITY_MIN)
          .setOngoing(true)
          .setShowWhen(false)
          .setOnlyAlertOnce(true)
          .setContentIntent(contentPendingIntent)
          .addAction(
            R.drawable.ic_delete_24dp,
            service.getString(R.string.notification_action_stop_v2ray),
            stopV2RayPendingIntent,
          )
          .addAction(
            R.drawable.ic_delete_24dp,
            service.getString(R.string.title_service_restart),
            restartV2RayPendingIntent,
          ).apply {
            if (!hideSaveOption) {
              addAction(
                R.drawable.ic_delete_24dp,
                service.getString(R.string.title_service_save),
                saveProfilePendingIntent,
              )
            }
          },
      )

    service.startForeground(NOTIFICATION_ID, notificationCompatBuilder?.build())
  }

  fun cancelNotification() {
    val service = getService() ?: return
    service.stopForeground(Service.STOP_FOREGROUND_REMOVE)

    speedTickRunnable?.let { speedHandler.removeCallbacks(it) }
    speedTickRunnable = null
    _notificationCompatBuilder?.clear()
    _notificationCompatBuilder = null
    mNotificationManager = null
  }

  fun stopSpeedNotification(connectionProfile: ConnectionProfile?) {
    speedTickRunnable?.let { speedHandler.removeCallbacks(it) }
    speedTickRunnable = null
    updateNotification(connectionProfile?.remarks, 0, 0)
  }

  @RequiresApi(Build.VERSION_CODES.O)
  private fun createNotificationChannel(): String {
    val channelId = AppConfig.RAY_NG_CHANNEL_ID
    val channelName = AppConfig.RAY_NG_CHANNEL_NAME
    val chan =
      NotificationChannel(
        channelId,
        channelName,
        SysNotificationManager.IMPORTANCE_HIGH,
      )
    chan.lightColor = Color.DKGRAY
    chan.importance = SysNotificationManager.IMPORTANCE_NONE
    chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
    getNotificationManager()?.createNotificationChannel(chan)
    return channelId
  }

  private fun updateNotification(
    contentText: String?,
    proxyTraffic: Long,
    directTraffic: Long,
  ) {
    if (notificationCompatBuilder != null) {
      if (proxyTraffic < NOTIFICATION_ICON_THRESHOLD && directTraffic < NOTIFICATION_ICON_THRESHOLD) {
        notificationCompatBuilder?.setSmallIcon(R.drawable.ic_stat_name)
      } else if (proxyTraffic > directTraffic) {
        notificationCompatBuilder?.setSmallIcon(R.drawable.ic_stat_proxy)
      } else {
        notificationCompatBuilder?.setSmallIcon(R.drawable.ic_stat_direct)
      }
      val svc = getService() ?: return
      val legend = svc.getString(R.string.notification_speed_legend)
      val fullText = if (contentText.isNullOrBlank()) legend else "$contentText\n$legend"
      notificationCompatBuilder?.setStyle(NotificationCompat.BigTextStyle().bigText(fullText))
      notificationCompatBuilder?.setContentText(contentText)
      getNotificationManager()?.notify(NOTIFICATION_ID, notificationCompatBuilder?.build())
    }
  }

  private fun getNotificationManager(): SysNotificationManager? {
    if (mNotificationManager == null) {
      val service = getService() ?: return null
      mNotificationManager = service.getSystemService(Context.NOTIFICATION_SERVICE) as SysNotificationManager
    }
    return mNotificationManager
  }

  private fun appendSpeedString(
    text: StringBuilder,
    name: String?,
    up: Double,
    down: Double,
  ) {
    var n = name ?: "no tag"
    n = n.take(min(n.length, 6))
    text.append(n)
    for (i in n.length..6 step 2) {
      text.append("\t")
    }
    text.append("\u2022  ${up.toLong().toSpeedString()}\u2191  ${down.toLong().toSpeedString()}\u2193\n")
  }

  private fun trafficBytes(
    stats: List<TrafficStats>,
    tag: String,
    direction: TrafficStats.Direction,
  ): Long {
    val raw =
      when (tag) {
        AppConfig.TAG_DIRECT ->
          stats
            .filterIsInstance<TrafficStats.Direct>()
            .firstOrNull { it.direction == direction }
            ?.bytes
        AppConfig.TAG_PROXY ->
          stats
            .filterIsInstance<TrafficStats.Proxy>()
            .firstOrNull { it.direction == direction }
            ?.bytes
        else ->
          stats
            .filterIsInstance<TrafficStats.Other>()
            .firstOrNull {
              it.value.contains(tag) &&
                it.value.contains(if (direction == TrafficStats.Direction.Up) AppConfig.UPLINK else AppConfig.DOWNLINK)
            }
            ?.value
            ?.split(",")
            ?.getOrNull(2)
      }
    return raw?.toLongOrNull() ?: 0L
  }

  private fun getService(): Service? = V2RayServiceManager.serviceControl?.get()?.getService()
}
