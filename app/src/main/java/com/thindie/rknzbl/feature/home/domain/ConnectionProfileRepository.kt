package com.thindie.rknzbl.feature.home.domain

import com.thindie.engine.core.WorkState
import com.v2ray.ang.dto.ConnectionProfile
import kotlinx.coroutines.flow.Flow

interface ConnectionProfileRepository {
  suspend fun read(): List<ConnectionProfile>

  suspend fun save(guid: String): Boolean

  suspend fun delete(profile: ConnectionProfile)

  fun autoSaved(): Flow<ConnectionProfile?>

  suspend fun saveAuto(guid: String)

  suspend fun markAutoSavedSeen()

  suspend fun fetchAutoSaved()

  suspend fun activeProfile(): ConnectionProfile?

  fun isSaved(profile: ConnectionProfile): Boolean

  fun invalidateCaches()

  /** Invalidate only the stored (local) profiles cache. */
  fun invalidateStoredCache()

  /** Invalidate only the remote source cache for a specific URL. */
  suspend fun invalidateRemoteCache(url: String)

  /** Read profiles from a specific remote source, using its dedicated cache. */
  suspend fun fetchFromSource(url: String): List<ConnectionProfile>

  // VPN service operations
  suspend fun connect(profile: ConnectionProfile)

  suspend fun disconnect()

  fun isConnected(): Boolean

  fun getConnectedServerName(): String

  val vpnState: Flow<WorkState>

  // Reactive API for connect -> fetch -> measure -> apply flow
  suspend fun fetch(force: Boolean)

  /**
   * Marks a pending connect intent so the reactive measurement path auto-connects once it yields
   * a best profile. Survives route recreation on purpose: a pending intent completes when the
   * user returns to Home.
   */
  fun requestConnect()

  /** Atomically reads and clears the pending connect intent. */
  fun takeConnectIntent(): Boolean

  val received: Flow<List<ConnectionProfile>?>
  val stored: Flow<List<ConnectionProfile>?>

  val lastMeasured: Flow<ConnectionProfile?>

  val connected: Flow<ConnectionProfile?>
}
