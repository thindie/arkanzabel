package com.thindie.rknzbl.application

import android.content.Context
import android.util.Log
import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.ConnectionProfile
import com.v2ray.ang.enums.Protocol
import com.v2ray.ang.error.AppError
import com.v2ray.ang.runtime.KeyValueStorage
import com.v2ray.ang.runtime.SettingsManager
import com.v2ray.ang.runtime.V2RayNativeManager
import com.v2ray.ang.runtime.V2rayConfigManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * Measures real outbound latency for connection profiles using the v2ray core.
 *
 * Two scopes are supported:
 *  - In-memory profiles (unsaved, fetched but not stored): measured on demand, never persisted.
 *  - Saved profiles (have a GUID): measured on demand and the result is persisted via
 *    [KeyValueStorage.encodeServerTestDelayMillis].
 *
 * [Protocol.Custom] and [Protocol.PolicyGroup] require a stored profile and are skipped.
 */
class ProfilePingManager(
  private val context: Context,
) {
  private val cpu = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
  private val dispatcher = Executors.newFixedThreadPool(cpu * 4).asCoroutineDispatcher()

  private val _results = MutableSharedFlow<Map<ConnectionProfile, Long>>(replay = 0, extraBufferCapacity = 12)
  val results: SharedFlow<Map<ConnectionProfile, Long>> = _results.asSharedFlow()

  /** Profiles currently being measured. Emits the live set so the UI can show a "checking" state. */
  private val _inFlight = MutableSharedFlow<Set<ConnectionProfile>>(replay = 0, extraBufferCapacity = 12)
  val inFlight: SharedFlow<Set<ConnectionProfile>> = _inFlight.asSharedFlow()

  private val savedResults = ConcurrentHashMap<String, Long>()

  private var batchJob: Job? = null

  /**
   * Measures latency for the given in-memory profiles in a batch and emits the combined results.
   *
   * Non-normal protocols ([Protocol.Custom], [Protocol.PolicyGroup]) are skipped. A result of
   * `-1` means the profile is unreachable.
   *
   * This is fire-and-forget: a new batch cancels the previous one. Callers must not suspend on it.
   */
  fun pingProfiles(profiles: List<ConnectionProfile>) {
    val pingable = profiles.filter { it.protocol.isPingable() }
    if (pingable.isEmpty()) return

    batchJob?.cancel()
    val job = Job()
    batchJob = job

    val resultsMap = ConcurrentHashMap<ConnectionProfile, Long>()
    val remaining = AtomicInteger(pingable.size)
    val inFlight = pingable.toSet()
    _inFlight.tryEmit(inFlight)

    CoroutineScope(dispatcher + job).launch {
      pingable.map { profile ->
        launch {
          try {
            resultsMap[profile] = measureInMemory(profile)
          } finally {
            if (remaining.decrementAndGet() == 0) {
              _results.tryEmit(resultsMap.toMap())
              _inFlight.tryEmit(emptySet())
            }
          }
        }
      }.joinAll()
    }
  }

  /**
   * Measures latency for a saved profile and persists the result.
   *
   * @return the measured delay in milliseconds, or `-1` on failure.
   */
  fun pingSaved(guid: String): Long {
    if (guid.isBlank()) return -1L
    val delay =
      try {
        val built = V2rayConfigManager.getV2rayConfig4Speedtest(context, guid)
        V2RayNativeManager.measureOutboundDelay(built.json, SettingsManager.getDelayTestUrl())
      } catch (c: CancellationException) {
        throw c
      } catch (e: AppError) {
        Log.w(AppConfig.TAG, "Speedtest config failed for $guid: ${e.userReadable}", e)
        -1L
      } catch (e: RuntimeException) {
        Log.w(AppConfig.TAG, "Speedtest config failed for $guid", e)
        -1L
      }
    savedResults[guid] = delay
    KeyValueStorage.encodeServerTestDelayMillis(guid, delay)
    return delay
  }

  private fun measureInMemory(profile: ConnectionProfile): Long {
    return try {
      val built = V2rayConfigManager.getV2rayConfig4Speedtest(context, profile)
      V2RayNativeManager.measureOutboundDelay(built.json, SettingsManager.getDelayTestUrl())
    } catch (c: CancellationException) {
      throw c
    } catch (e: AppError) {
      Log.w(AppConfig.TAG, "Speedtest config failed for ${profile.remarks}: ${e.userReadable}", e)
      -1L
    } catch (e: RuntimeException) {
      Log.w(AppConfig.TAG, "Speedtest config failed for ${profile.remarks}", e)
      -1L
    }
  }

  private fun Protocol.isPingable(): Boolean =
    when (this) {
      Protocol.Custom, Protocol.PolicyGroup -> false
      else -> true
    }
}
