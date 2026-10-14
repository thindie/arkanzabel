package com.v2ray.ang.ipc

import android.content.Intent
import com.thindie.engine.core.Log
import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.ConnectionProfile
import com.v2ray.ang.util.JsonUtil

fun handleDaemonEvent(event: DaemonToMain): Intent? {
  val code =
    when (event) {
      is DaemonToMain.Running -> AppConfig.MSG_STATE_RUNNING
      is DaemonToMain.NotRunning -> AppConfig.MSG_STATE_NOT_RUNNING
      is DaemonToMain.StartSuccess -> AppConfig.MSG_STATE_START_SUCCESS
      is DaemonToMain.StartFailure -> AppConfig.MSG_STATE_START_FAILURE
      is DaemonToMain.StopSuccess -> AppConfig.MSG_STATE_STOP_SUCCESS
      is DaemonToMain.SaveProfile -> AppConfig.MSG_STATE_SAVE_PROFILE
      is DaemonToMain.ProfileMeasured -> AppConfig.MSG_MEASURE_PROFILE_DONE
    }

  return try {
    Intent().apply {
      action = AppConfig.BROADCAST_ACTION_ACTIVITY
      `package` = AppConfig.ANG_PACKAGE
      putExtra(EXTRA_KEY, code)

      when (event) {
        is DaemonToMain.StartSuccess -> putProfile(event.profile)
        is DaemonToMain.StartFailure -> putExtra(EXTRA_CONTENT, event.message)
        is DaemonToMain.NotRunning -> Unit
        is DaemonToMain.Running -> Unit
        is DaemonToMain.SaveProfile -> Unit
        is DaemonToMain.StopSuccess -> Unit
        is DaemonToMain.ProfileMeasured -> {
          putProfile(event.profile)
          putExtra(EXTRA_REQUEST_ID, event.requestId)
        }
      }
    }
  } catch (e: Exception) {
    Log.e({ "Failed to emit daemon event: \$event" }, AppConfig.TAG, e)
    null
  }
}

internal fun Intent.getMainExtra(): FromMainToDaemon? {
  if (action != AppConfig.BROADCAST_ACTION_SERVICE) return null

  if (`package` != AppConfig.ANG_PACKAGE) return null

  val code = getIntExtra(EXTRA_KEY, 0)
  Log.i({ "DaemonServer received code=\$code" }, AppConfig.TAG_KERNEL)

  return when (code) {
    AppConfig.MSG_REGISTER_CLIENT -> FromMainToDaemon.RegisterClient
    AppConfig.MSG_UNREGISTER_CLIENT -> FromMainToDaemon.UnregisterClient
    // Missing/malformed profile extra: drop the command instead of crashing on a foreign intent.
    AppConfig.MSG_STATE_START -> getProfile()?.let { FromMainToDaemon.Start(it) }
    AppConfig.MSG_STATE_STOP -> FromMainToDaemon.Stop
    AppConfig.MSG_STATE_RESTART -> FromMainToDaemon.Restart
    AppConfig.MSG_STATE_SAVE_PROFILE -> FromMainToDaemon.SaveProfile
    AppConfig.MSG_MEASURE_PROFILE ->
      getProfile()?.let { profile ->
        getStringExtra(EXTRA_REQUEST_ID)
          ?.takeIf { it.isNotBlank() }
          ?.let { requestId -> FromMainToDaemon.MeasureProfile(profile, requestId) }
      }
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
    is FromMainToDaemon.SaveProfile -> AppConfig.MSG_STATE_SAVE_PROFILE
    is FromMainToDaemon.MeasureProfile -> AppConfig.MSG_MEASURE_PROFILE
  }

internal fun Intent.putCoreExtra(command: FromMainToDaemon) {
  putExtra(EXTRA_KEY, commandToCode(command))
  `package` = AppConfig.ANG_PACKAGE
  when (command) {
    is FromMainToDaemon.Start -> putProfile(command.profile)
    is FromMainToDaemon.MeasureProfile -> {
      putProfile(command.profile)
      putExtra(EXTRA_REQUEST_ID, command.requestId)
    }
    else -> Unit
  }
}

internal fun Intent.getCoreExtra(): DaemonToMain? {
  if (this.`package` != AppConfig.ANG_PACKAGE) return null

  val code = getIntExtra(EXTRA_KEY, 0)
  return when (code) {
    AppConfig.MSG_STATE_RUNNING -> DaemonToMain.Running
    AppConfig.MSG_STATE_NOT_RUNNING -> DaemonToMain.NotRunning
    // Missing/malformed profile extra: drop the event instead of crashing on a foreign intent.
    AppConfig.MSG_STATE_START_SUCCESS -> getProfile()?.let { DaemonToMain.StartSuccess(it) }
    AppConfig.MSG_STATE_START_FAILURE ->
      DaemonToMain.StartFailure(getStringExtra(EXTRA_CONTENT) ?: "Unknown error")

    AppConfig.MSG_STATE_STOP_SUCCESS -> DaemonToMain.StopSuccess
    AppConfig.MSG_MEASURE_PROFILE_DONE ->
      getProfile()?.let { profile ->
        getStringExtra(EXTRA_REQUEST_ID)
          ?.takeIf { it.isNotBlank() }
          ?.let { requestId -> DaemonToMain.ProfileMeasured(profile, requestId) }
      }

    AppConfig.MSG_STATE_SAVE_PROFILE -> DaemonToMain.SaveProfile
    else -> null
  }
}

internal fun Intent.putProfile(connectionProfile: ConnectionProfile) {
  val json = JsonUtil.toJson(connectionProfile)
  putExtra(EXTRA_PROFILE, json)
}

internal fun Intent.getProfile(): ConnectionProfile? =
  getStringExtra(EXTRA_PROFILE)
    ?.ifBlank { null }
    ?.let { JsonUtil.fromJsonOrNull(it, ConnectionProfile::class.java) }

private const val EXTRA_KEY = "key"
private const val EXTRA_PROFILE = "profileJson"
private const val EXTRA_REQUEST_ID = "profilePingRequestId"
private const val EXTRA_CONTENT = "content"
