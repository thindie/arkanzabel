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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.coroutines.resumeWithException

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
  private val scope: CoroutineScope,
) {
  private val cpu = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
  private val dispatcher = Executors.newFixedThreadPool(cpu * 4).asCoroutineDispatcher()

  private data class State(
    val results: Map<ConnectionProfile, Long>? = null,
    val lastMeasured: Pair<ConnectionProfile, Long>? = null,
  )

  private val stateFlow = MutableStateFlow(State())

  val lastMeasured =
    stateFlow
      .map { it.lastMeasured }
      .distinctUntilChanged()

  val measureResults =
    stateFlow
      .mapNotNull { it.results }
      .onEach { stateFlow.update { State() } }

  private val savedResults = ConcurrentHashMap<String, Long>()

  private var batchJob: Job? = null

  fun pingProfiles(
    profiles: List<ConnectionProfile>,
    force: Boolean,
  ) {
    val pingable = profiles.filter { it.protocol.isPingable() }
    if (pingable.isEmpty()) {
      return
    }
    batchJob?.cancel()
    batchJob = null
    batchJob =
      if (force) {
        scope.launch(dispatcher) {
          val ping =
            pingable.map { profile -> async { profile to measureInMemory(profile, force) } }
              .awaitAll()
              .toMap()

          stateFlow.update {
            it.copy(results = ping)
          }
        }
      } else {
        scope.launch(Dispatchers.Default) {
          val ping =
            pingable.associate { profile ->
              val measured = profile to measureInMemory(profile, force)
              stateFlow.update {
                it.copy(lastMeasured = measured)
              }
              measured
            }

          stateFlow.update {
            it.copy(results = ping)
          }
        }
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

  private suspend fun measureInMemory(
    profile: ConnectionProfile,
    force: Boolean,
  ): Long {
    return try {
      val built = V2rayConfigManager.getV2rayConfig4Speedtest(context, profile)
      if (force) {
        V2RayNativeManager.measureOutboundDelay(built.json, SettingsManager.getDelayTestUrl())
      } else {
        suspendCancellableCoroutine {
          try {
            it.resume(
              V2RayNativeManager
                .measureOutboundDelay(built.json, SettingsManager.getDelayTestUrl()),
            ) { cause, _, _ ->
              throw cause
            }
          } catch (e: Throwable) {
            it.resumeWithException(e)
          }
        }
      }
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
