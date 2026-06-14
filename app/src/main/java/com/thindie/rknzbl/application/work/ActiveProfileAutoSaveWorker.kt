package com.thindie.rknzbl.application.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.thindie.rknzbl.error.AppError
import com.thindie.rknzbl.feature.home.domain.ConnectionProfileRepository
import com.v2ray.ang.AppConfig
import com.v2ray.ang.runtime.KeyValueStorage

class ActiveProfileAutoSaveWorker(
  appContext: Context,
  params: WorkerParameters,
  private val repository: ConnectionProfileRepository,
) : CoroutineWorker(appContext, params) {
  override suspend fun doWork(): Result {
    val enabledRaw =
      KeyValueStorage.decodeSettingsString(AppConfig.PREF_AUTO_SAVE_ACTIVE_PROFILE_ENABLED, "1")
    if (enabledRaw == "0") {
      return Result.success()
    }

    val hoursRaw =
      KeyValueStorage.decodeSettingsString(
        AppConfig.PREF_AUTO_SAVE_ACTIVE_PROFILE_HOURS,
        AppConfig.AUTO_SAVE_ACTIVE_PROFILE_DEFAULT_HOURS,
      )
    val hours = hoursRaw?.toLongOrNull()?.coerceIn(1L, 720L) ?: 24L
    val thresholdMs = hours * 3_600_000L

    if (!KeyValueStorage.isVpnSessionActive()) {
      return Result.success()
    }
    val guid = KeyValueStorage.getVpnSessionGuid()
    if (guid.isNullOrBlank()) {
      return Result.success()
    }
    val sessionStartMs = KeyValueStorage.getVpnSessionStartEpochMs()
    if (sessionStartMs <= 0L) {
      return Result.success()
    }

    val elapsed = System.currentTimeMillis() - sessionStartMs
    if (elapsed < thresholdMs) {
      return Result.success()
    }

    if (KeyValueStorage.getVpnSessionLastAutoSaveStartMs() == sessionStartMs) {
      return Result.success()
    }

    return try {
      val isSaved = repository.save(guid)
      if (isSaved) {
        KeyValueStorage.setVpnSessionLastAutoSaveStartMs(sessionStartMs)
        repository.saveAuto(guid)
      }
      Result.success()
    } catch (_: AppError.ServerError) {
      Result.retry()
    }
  }
}
