package com.thindie.engine.core

import android.app.ActivityManager
import android.content.Context
import android.os.Process

enum class ProcessKind {
  Main,
  Daemon,
}

fun Context.determineProcess(): ProcessKind {
  val activityManager = getSystemService(ActivityManager::class.java)
  val current =
    activityManager.runningAppProcesses
      ?.firstOrNull { it.pid == Process.myPid() }
      ?.processName

  return if (current?.endsWith(DAEMON_PROCESS_SUFFIX) != true) ProcessKind.Main else ProcessKind.Daemon
}

private const val DAEMON_PROCESS_SUFFIX = ":RunSoLibV2RayDaemon"
