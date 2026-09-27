package com.v2ray.ang.ipc

import android.content.Intent
import com.thindie.engine.core.Log
import com.v2ray.ang.AppConfig

fun handleDaemonEvent(event: DaemonToMain): Intent? {
  val code =
    when (event) {
      is DaemonToMain.Running -> AppConfig.MSG_STATE_RUNNING
      is DaemonToMain.NotRunning -> AppConfig.MSG_STATE_NOT_RUNNING
      is DaemonToMain.StartSuccess -> AppConfig.MSG_STATE_START_SUCCESS
      is DaemonToMain.StartFailure -> AppConfig.MSG_STATE_START_FAILURE
      is DaemonToMain.StopSuccess -> AppConfig.MSG_STATE_STOP_SUCCESS
      is DaemonToMain.DelayMeasured -> AppConfig.MSG_MEASURE_DELAY_SUCCESS
      is DaemonToMain.ConfigTestResult -> AppConfig.MSG_MEASURE_CONFIG_SUCCESS
      is DaemonToMain.ConfigTestProgress -> AppConfig.MSG_MEASURE_CONFIG_NOTIFY
      is DaemonToMain.ConfigTestFinished -> AppConfig.MSG_MEASURE_CONFIG_FINISH
      is DaemonToMain.SaveProfile -> AppConfig.MSG_STATE_SAVE_PROFILE
    }

  return try {
    Intent().apply {
      action = AppConfig.BROADCAST_ACTION_ACTIVITY
      `package` = AppConfig.ANG_PACKAGE
      putExtra(EXTRA_KEY, code)

      when (event) {
        is DaemonToMain.StartSuccess -> event.guid?.let { putExtra(EXTRA_GUID, it) }
        is DaemonToMain.StartFailure -> putExtra(EXTRA_CONTENT, event.message)
        is DaemonToMain.DelayMeasured -> {
          event.guid?.let { putExtra(EXTRA_GUID, it) }
          event.delayMs?.let { putExtra(EXTRA_DELAY_MS, it) }
        }
        is DaemonToMain.ConfigTestResult -> {
          putExtra(EXTRA_GUID, event.guid)
          putExtra(EXTRA_RESULT, event.result ?: 0L)
        }
        is DaemonToMain.ConfigTestProgress -> {
          putExtra(EXTRA_REMAINING, event.remaining)
          putExtra(EXTRA_TOTAL, event.total)
        }
        is DaemonToMain.ConfigTestFinished -> Unit
        is DaemonToMain.NotRunning -> Unit
        is DaemonToMain.Running -> Unit
        is DaemonToMain.SaveProfile -> Unit
        is DaemonToMain.StopSuccess -> Unit
      }
    }
  } catch (e: Exception) {
    Log.e({ "Failed to emit daemon event: \$event" }, AppConfig.TAG, e)
    null
  }
}

internal fun handleIntent(intent: Intent): FromMainToDaemon? {
  if (intent.action != AppConfig.BROADCAST_ACTION_SERVICE) return null

  if (intent.`package` != AppConfig.ANG_PACKAGE) return null

  val code = intent.getIntExtra(EXTRA_KEY, 0)
  Log.i({ "DaemonServer received code=\$code" }, AppConfig.TAG_KERNEL)

  return when (code) {
    AppConfig.MSG_REGISTER_CLIENT -> FromMainToDaemon.RegisterClient
    AppConfig.MSG_UNREGISTER_CLIENT -> FromMainToDaemon.UnregisterClient
    AppConfig.MSG_STATE_START -> FromMainToDaemon.Start(guidFrom(intent))
    AppConfig.MSG_STATE_STOP -> FromMainToDaemon.Stop
    AppConfig.MSG_STATE_RESTART -> FromMainToDaemon.Restart
    AppConfig.MSG_MEASURE_DELAY -> FromMainToDaemon.MeasureDelay(guidFrom(intent))
    AppConfig.MSG_STATE_SAVE_PROFILE -> FromMainToDaemon.SaveProfile
    else -> null
  }
}

private fun guidFrom(intent: Intent): String? = intent
    .getStringExtra(EXTRA_GUID)
    ?.ifBlank { null }

private fun commandToCode(command: FromMainToDaemon): Int =
  when (command) {
    is FromMainToDaemon.RegisterClient -> AppConfig.MSG_REGISTER_CLIENT
    is FromMainToDaemon.UnregisterClient -> AppConfig.MSG_UNREGISTER_CLIENT
    is FromMainToDaemon.Start -> AppConfig.MSG_STATE_START
    is FromMainToDaemon.Stop -> AppConfig.MSG_STATE_STOP
    is FromMainToDaemon.Restart -> AppConfig.MSG_STATE_RESTART
    is FromMainToDaemon.MeasureDelay -> AppConfig.MSG_MEASURE_DELAY
    is FromMainToDaemon.SaveProfile -> AppConfig.MSG_STATE_SAVE_PROFILE
  }

internal fun Intent.putCoreExtra(command: FromMainToDaemon) {
  putExtra(EXTRA_KEY, commandToCode(command))
  `package` = AppConfig.ANG_PACKAGE
  when (command) {
    is FromMainToDaemon.Start -> command.guid?.let { putExtra(EXTRA_GUID, it) }
    is FromMainToDaemon.MeasureDelay -> command.guid?.let { putExtra(EXTRA_GUID, it) }
    else -> Unit
  }
}

internal fun Intent.getCoreExtra(): DaemonToMain? {
  if (this.`package` != AppConfig.ANG_PACKAGE) return null

  val code = getIntExtra(EXTRA_KEY, 0)
  return when (code) {
    AppConfig.MSG_STATE_RUNNING -> DaemonToMain.Running
    AppConfig.MSG_STATE_NOT_RUNNING -> DaemonToMain.NotRunning
    AppConfig.MSG_STATE_START_SUCCESS -> DaemonToMain.StartSuccess(guidFrom(this))
    AppConfig.MSG_STATE_START_FAILURE ->
      DaemonToMain.StartFailure(getStringExtra(EXTRA_CONTENT) ?: "Unknown error")

    AppConfig.MSG_STATE_STOP_SUCCESS -> DaemonToMain.StopSuccess
    AppConfig.MSG_MEASURE_DELAY_SUCCESS -> {
      val guid = guidFrom(this)
      val delayMs = getLongExtra(EXTRA_DELAY_MS, -1L).takeIf { it >= 0 }
      DaemonToMain.DelayMeasured(guid, delayMs)
    }

    AppConfig.MSG_MEASURE_CONFIG_SUCCESS -> {
      val guid = getStringExtra(EXTRA_GUID) ?: return null
      val result = getLongExtra(EXTRA_RESULT, -1L)
      DaemonToMain.ConfigTestResult(guid, result)
    }

    AppConfig.MSG_MEASURE_CONFIG_NOTIFY -> {
      val remaining = getIntExtra(EXTRA_REMAINING, 0)
      val total = getIntExtra(EXTRA_TOTAL, 0)
      DaemonToMain.ConfigTestProgress(remaining, total)
    }

    AppConfig.MSG_MEASURE_CONFIG_FINISH -> DaemonToMain.ConfigTestFinished
    AppConfig.MSG_STATE_SAVE_PROFILE -> DaemonToMain.SaveProfile
    else -> null
  }
}

private const val EXTRA_KEY = "key"
private const val EXTRA_GUID = "guid"
private const val EXTRA_CONTENT = "content"
private const val EXTRA_DELAY_MS = "delayMs"
private const val EXTRA_RESULT = "result"
private const val EXTRA_REMAINING = "remaining"
private const val EXTRA_TOTAL = "total"
