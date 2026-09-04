package com.thindie.rknzbl.feature.home.domain

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
  suspend fun readFromSource(url: String): List<ConnectionProfile>

  /** Invalidate only the stored (local) profiles cache. */
  fun invalidateStoredCache()

  /** Invalidate only the remote source cache for a specific URL. */
  suspend fun invalidateRemoteCache(url: String)

  /** Read profiles from a specific remote source, using its dedicated cache. */
  suspend fun readFromSource(url: String): List<ConnectionProfile>

  // VPN service operations
  suspend fun connect(profile: ConnectionProfile)

  suspend fun disconnect()

  fun isConnected(): Boolean

  fun getConnectedServerName(): String

  // Reactive API for connect -> fetch -> measure -> apply flow
  suspend fun fetch()

  val profiles: Flow<List<ConnectionProfile>>
  val stored: Flow<List<ConnectionProfile>>

  val measured: Flow<ConnectionProfile?>

  val connected: Flow<Boolean>
}
