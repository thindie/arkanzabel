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
    }

  val content =
    when (event) {
      is DaemonToMain.StartSuccess -> event.guid.orEmpty()
      is DaemonToMain.StartFailure -> event.message
      is DaemonToMain.DelayMeasured -> "${event.guid},${event.delayMs}"
      is DaemonToMain.ConfigTestResult -> event.guid
      is DaemonToMain.ConfigTestProgress -> "${event.remaining}/${event.total}"
      else -> ""
    }

  return try {
    val intent = Intent()
    intent.action = AppConfig.BROADCAST_ACTION_ACTIVITY
    intent.`package` = AppConfig.ANG_PACKAGE
    intent.putExtra(EXTRA_KEY, code)
    intent.putExtra(EXTRA_CONTENT, content)
    intent
  } catch (e: Exception) {
    Log.e({ "Failed to emit daemon event: $event" }, AppConfig.TAG, e)
    null
  }
}

internal fun handleIntent(intent: Intent): FromMainToDaemon? {
  if (intent.action != AppConfig.BROADCAST_ACTION_SERVICE) return null
  val code = intent.getIntExtra(EXTRA_KEY, 0)
  val content = intent.getStringExtra(EXTRA_CONTENT)

  Log.i({ "DaemonServer received code=$code" }, AppConfig.TAG_KERNEL)

  return when (code) {
    AppConfig.MSG_REGISTER_CLIENT -> FromMainToDaemon.RegisterClient
    AppConfig.MSG_UNREGISTER_CLIENT -> FromMainToDaemon.UnregisterClient
    AppConfig.MSG_STATE_START -> FromMainToDaemon.Start(content)
    AppConfig.MSG_STATE_STOP -> FromMainToDaemon.Stop
    AppConfig.MSG_STATE_RESTART -> FromMainToDaemon.Restart
    AppConfig.MSG_MEASURE_DELAY -> FromMainToDaemon.MeasureDelay(content)
    AppConfig.MSG_STATE_SAVE_PROFILE -> FromMainToDaemon.SaveProfile
    else -> null
  }
}

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

private fun codeToEvent(
  code: Int,
  content: Any?,
): DaemonToMain? =
  when (code) {
    AppConfig.MSG_STATE_RUNNING -> DaemonToMain.Running
    AppConfig.MSG_STATE_NOT_RUNNING -> DaemonToMain.NotRunning
    AppConfig.MSG_STATE_START_SUCCESS -> DaemonToMain.StartSuccess(content as? String)
    AppConfig.MSG_STATE_START_FAILURE ->
      DaemonToMain.StartFailure(content?.toString() ?: "Unknown error")
    AppConfig.MSG_STATE_STOP_SUCCESS -> DaemonToMain.StopSuccess
    AppConfig.MSG_MEASURE_DELAY_SUCCESS -> {
      val parts = (content as? String)?.split(",")
      if (parts != null && parts.size >= 2) {
        DaemonToMain.DelayMeasured(parts[0], parts[1].toLongOrNull())
      } else {
        null
      }
    }

    AppConfig.MSG_MEASURE_CONFIG_SUCCESS -> content?.let { DaemonToMain.ConfigTestResult(it.toString(), null) }
    AppConfig.MSG_MEASURE_CONFIG_NOTIFY -> {
      val parts = (content as? String)?.split("/")
      if (parts != null && parts.size == 2) {
        DaemonToMain.ConfigTestProgress(parts[0].toIntOrNull() ?: 0, parts[1].toIntOrNull() ?: 0)
      } else {
        null
      }
    }

    AppConfig.MSG_MEASURE_CONFIG_FINISH -> DaemonToMain.ConfigTestFinished
    else -> null
  }

internal fun Intent.putCoreExtra(command: FromMainToDaemon) {
  val code = commandToCode(command)
  putExtra(EXTRA_KEY, code)
  putCoreExtraContent(command)
}

private fun Intent.putCoreExtraContent(command: FromMainToDaemon) {
  val content =
    when (command) {
      is FromMainToDaemon.MeasureDelay -> command.guid.orEmpty()
      else -> BLANK_CONTENT
    }
  putExtra(EXTRA_CONTENT, content)
}

internal fun Intent.getCoreExtra(): DaemonToMain? {
  val code = getIntExtra(EXTRA_KEY, 0)
  val content = getStringExtra(EXTRA_CONTENT)
  return codeToEvent(code, content)
}

private const val EXTRA_KEY = "key"
private const val BLANK_CONTENT = ""
private const val EXTRA_CONTENT = "content"
