package com.thindie.rknzbl.application

import android.content.Context
import com.thindie.engine.core.Log
import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.ConnectionProfile
import com.v2ray.ang.enums.Protocol
import com.v2ray.ang.error.AppError
import com.v2ray.ang.runtime.KeyValueStorage
import com.v2ray.ang.runtime.SettingsManager
import com.v2ray.ang.runtime.SpeedtestManager
import com.v2ray.ang.runtime.V2RayNativeManager
import com.v2ray.ang.runtime.V2rayConfigManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import kotlin.coroutines.resume

/**
 * Measures real outbound latency for connection profiles using the v2ray core.
 *
 * Two scopes are supported:
 *  - In-memory profiles (unsaved, fetched but not stored): measured on demand, never persisted.
 *    The full batch result is published via [batch] (tagged with a monotonically increasing id so
 *    consumers can ignore stale results); incremental progress is available via [lastMeasured].
 *  - Saved profiles (have a GUID): measured asynchronously by [pingSaved] and the result is
 *    persisted via [KeyValueStorage.encodeServerTestDelayMillis].
 *
 * Every measurement is bounded by [PER_PROFILE_TIMEOUT_MS]: on timeout the profile gets
 * [FAILED_DELAY_MS] and the batch continues. The native call itself cannot be interrupted, so a
 * timed-out measurement keeps occupying a pool thread until it returns — its result is simply
 * discarded.
 *
 * Delay convention: 0 = not measured, [FAILED_DELAY_MS] = unreachable/failed.
 *
 * [Protocol.Custom] and [Protocol.PolicyGroup] require a stored profile and are skipped.
 */
class ProfilePingManager(
  private val context: Context,
  private val scope: CoroutineScope,
) {
  companion object {
    /** Marks a failed measurement; storage convention treats < 0 as unreachable. */
    const val FAILED_DELAY_MS = -1L

    /** Wall-time bound for one profile's speedtest call. */
    const val PER_PROFILE_TIMEOUT_MS = 15_000L

    /** TTL for cached S2 latency results. */
    private const val S2_CACHE_TTL_MS = 5 * 60 * 1000L // 5 minutes
  }

  /** One completed batch: id is monotonically increasing per [pingProfiles] invocation. */
  data class BatchResult(
    val id: Long,
    val results: Map<ConnectionProfile, Long>,
  )

  private val cpu = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
  private val pool = Executors.newFixedThreadPool(cpu * 4)
  private val dispatcher = pool.asCoroutineDispatcher()

  /** Last completed batch; null until the first batch finishes. */
  private val _batch = MutableStateFlow<BatchResult?>(null)

  val batch: StateFlow<BatchResult?> = _batch

  /** Emits the full result map once per completed batch (no replay — late subscribers get nothing). */
  private val resultsFlow =
    MutableSharedFlow<Map<ConnectionProfile, Long>>(
      replay = 0,
      extraBufferCapacity = 1,
      onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

  val measureResults: SharedFlow<Map<ConnectionProfile, Long>> = resultsFlow

  /** Incremental progress: last completed (profile, delay) pair of the running sequential batch. */
  private val _lastMeasured = MutableStateFlow<Pair<ConnectionProfile, Long>?>(null)

  val lastMeasured: StateFlow<Pair<ConnectionProfile, Long>?> = _lastMeasured

  private var batchJob: Job? = null
  private var nextBatchId = 0L

  // S2 latency cache: subscriptionId -> (delayMs, timestamp)
  private val s2LatencyCache = mutableMapOf<String, Pair<Long, Long>>()
  private val s2CacheLock = Any()

  /** Get cached S2 latency if fresh enough; null otherwise. */
  fun getCachedLatency(profile: ConnectionProfile): Long? {
    synchronized(s2CacheLock) {
      val entry = s2LatencyCache[profile.subscriptionId] ?: return null
      val (delay, timestamp) = entry
      if (System.currentTimeMillis() - timestamp > S2_CACHE_TTL_MS) {
        s2LatencyCache.remove(profile.subscriptionId)
        return null
      }
      return delay
    }
  }

  /** Store S2 latency result in cache. */
  private fun cacheLatency(
    profile: ConnectionProfile,
    delayMs: Long,
  ) {
    if (delayMs < 0) return // Don't cache failures
    synchronized(s2CacheLock) {
      s2LatencyCache[profile.subscriptionId] = delayMs to System.currentTimeMillis()
    }
  }

  /** Invalidate all cached measurement results. Called on repository cache invalidation. */
  fun invalidateMeasurementCache() {
    synchronized(s2CacheLock) {
      s2LatencyCache.clear()
    }
  }

  /** Starts a measurement batch and returns its id; -1 when nothing is pingable. */
  fun pingProfiles(
    profiles: List<ConnectionProfile>,
    force: Boolean,
  ): Long {
    val pingable = profiles.filter { it.protocol.isPingable() }
    if (pingable.isEmpty()) {
      return FAILED_DELAY_MS
    }
    batchJob?.cancel()
    _lastMeasured.value = null
    val batchId = ++nextBatchId
    batchJob =
      scope.launch(dispatcher) {
        val results =
          if (force) {
            pingable.map { profile -> async { profile to measureInMemory(profile) } }.awaitAll().toMap()
          } else {
            val sequential = LinkedHashMap<ConnectionProfile, Long>()
            for (profile in pingable) {
              ensureActive()
              val delay = measureInMemory(profile)
              _lastMeasured.value = profile to delay
              sequential[profile] = delay
            }
            sequential
          }
        _batch.value = BatchResult(batchId, results)
        resultsFlow.tryEmit(results)
      }
    return batchId
  }

  /**
   * Measures latency for a saved profile and persists the result.
   *
   * Fire-and-forget: returns immediately; the measurement runs on the ping pool so callers must
   * not block UI on it.
   */
  fun pingSaved(guid: String) {
    if (guid.isBlank()) return
    scope.launch(dispatcher) {
      val delay = measureSavedDelay(guid)
      KeyValueStorage.encodeServerTestDelayMillis(guid, delay)
    }
  }

  private suspend fun measureInMemory(profile: ConnectionProfile): Long {
    val json =
      buildSpeedtestConfig(profile.remarks) { V2rayConfigManager.getV2rayConfig4Speedtest(context, profile).json }
        ?: return FAILED_DELAY_MS
    val delay = measureOutboundDelay(json, profile.remarks)
    if (delay >= 0) cacheLatency(profile, delay)
    return delay
  }

  private suspend fun measureSavedDelay(guid: String): Long {
    val json =
      buildSpeedtestConfig(guid) { V2rayConfigManager.getV2rayConfig4Speedtest(context, guid).json }
        ?: return FAILED_DELAY_MS
    return measureOutboundDelay(json, guid)
  }

  /** Builds the speedtest core JSON; returns null (and logs) when config assembly fails. */
  private fun <T> buildSpeedtestConfig(
    label: String,
    build: () -> T,
  ): T? {
    return try {
      build()
    } catch (c: CancellationException) {
      throw c
    } catch (e: AppError) {
      Log.w({ "Speedtest config failed for $label: ${e.userReadable}" }, AppConfig.TAG, e)
      null
    } catch (e: RuntimeException) {
      Log.w({ "Speedtest config failed for $label" }, AppConfig.TAG, e)
      null
    }
  }

  /**
   * Runs the blocking native speedtest on [pool] and awaits it with a wall-time bound.
   * On timeout or task failure returns [FAILED_DELAY_MS]; batch cancellation rethrows.
   */
  private suspend fun measureOutboundDelay(
    builtJson: String,
    label: String,
  ): Long {
    val future =
      CompletableFuture.supplyAsync(
        { V2RayNativeManager.measureOutboundDelay(builtJson, SettingsManager.getDelayTestUrl()) },
        pool,
      )
    return try {
      withTimeout(PER_PROFILE_TIMEOUT_MS) {
        suspendCancellableCoroutine { cont ->
          future.whenComplete { value, error ->
            if (error == null) {
              cont.resume(value)
            } else {
              Log.w({ "Speedtest task failed for $label" }, AppConfig.TAG, error)
              cont.resume(FAILED_DELAY_MS)
            }
          }
        }
      }
    } catch (t: TimeoutCancellationException) {
      Log.w({ "Speedtest timed out for $label after ${PER_PROFILE_TIMEOUT_MS}ms" }, AppConfig.TAG)
      FAILED_DELAY_MS
    } catch (c: CancellationException) {
      throw c
    }
  }

  /**
   * Measures all pingable [profiles] in parallel and returns the one with the lowest delay, or
   * null when none is reachable. The result is published as a completed batch so consumers of
   * [batch]/[measureResults] (best-profile reconnect, fresh delay lists) see it like any other run.
   */
  suspend fun measure(profiles: List<ConnectionProfile>): ConnectionProfile? {
    val pingable = profiles.filter { it.protocol.isPingable() }
    if (pingable.isEmpty()) return null
    val results =
      coroutineScope {
        pingable.map { profile -> async(dispatcher) { profile to measureInMemory(profile) } }.awaitAll().toMap()
      }
    _batch.value = BatchResult(++nextBatchId, results)
    resultsFlow.tryEmit(results)
    return results.filterValues { it >= 0 }.minByOrNull { it.value }?.key
  }

  /**
   * Three-stage measurement pipeline for auto-connect.
   * Stage 1 filters dead servers, stage 2 measures real latency,
   * stage 3 measures throughput on top candidates only.
   */
  suspend fun measureStaged(
    profiles: List<ConnectionProfile>,
    topNForBandwidth: Int = 3,
  ): ConnectionProfile? {
    val pingable = profiles.filter { it.protocol.isPingable() }
    if (pingable.isEmpty()) return null

    // Stage 1: TCP reachability - all in parallel, fast timeout
    val reachableProfiles =
      coroutineScope {
        pingable.map { profile ->
          async(dispatcher) {
            val server = profile.server ?: return@async ProfileMeasurement(profile)
            val port = profile.serverPort?.toIntOrNull() ?: return@async ProfileMeasurement(profile)
            val tcpMs = SpeedtestManager.socketConnectTime(server, port)
            ProfileMeasurement(
              profile = profile,
              reachable = tcpMs > 0,
              tcpConnectMs = tcpMs,
            )
          }
        }.awaitAll().filter { it.reachable }
      }

    if (reachableProfiles.isEmpty()) return null

    // Stage 2: Full protocol latency - only reachable profiles, use cache when fresh
    val withLatency =
      coroutineScope {
        reachableProfiles.map { m ->
          async(dispatcher) {
            val latency = getCachedLatency(m.profile) ?: measureInMemory(m.profile)
            m.copy(latencyMs = latency)
          }
        }.awaitAll().filter { it.latencyMs != null && it.latencyMs >= 0 }
      }

    if (withLatency.isEmpty()) return reachableProfiles.minByOrNull { it.tcpConnectMs!! }?.profile

    // Stage 3: Bandwidth - only top N by latency
    val sorted = withLatency.sortedBy { it.latencyMs!! }
    val candidatesForBandwidth = sorted.take(topNForBandwidth)

    val finalResults =
      coroutineScope {
        candidatesForBandwidth.map { m ->
          async(dispatcher) {
            val port = m.profile.serverPort?.toIntOrNull()
            var bw: Long? = null
            if (port != null) {
              try {
                bw = SpeedtestManager.measureBandwidth(port)
              } catch (e: Exception) {
                Log.w({ "Bandwidth test failed for ${m.profile.remarks}" }, AppConfig.TAG, e)
              }
            }
            m.copy(bandwidthBps = bw)
          }
        }.awaitAll()
      }

    // Final selection: prefer lowest latency, break ties with highest bandwidth
    return finalResults.minByOrNull {
      val lat = it.latencyMs!!
      val bw = (it.bandwidthBps ?: 1L).coerceAtLeast(1L)
      // Weighted score: latency dominates, bandwidth as tie-breaker
      lat * 1000 / bw
    }?.profile
  }

  private fun Protocol.isPingable(): Boolean =
    when (this) {
      Protocol.Custom, Protocol.PolicyGroup -> false
      else -> true
    }
}

/** Result of a single profile's multi-stage measurement. */
data class ProfileMeasurement(
  val profile: ConnectionProfile,
  val reachable: Boolean = false,
  val tcpConnectMs: Long? = null,
  val latencyMs: Long? = null,
  val bandwidthBps: Long? = null,
)
