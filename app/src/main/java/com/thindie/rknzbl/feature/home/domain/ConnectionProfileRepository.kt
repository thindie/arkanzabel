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

  // VPN service operations
  suspend fun connect(profile: ConnectionProfile)

  suspend fun disconnect()

  fun isConnected(): Boolean

  fun getConnectedServerName(): String

  // Reactive API for connect -> fetch -> measure -> apply flow
  suspend fun fetch()

  val profiles: Flow<List<ConnectionProfile>>

  val measured: Flow<ConnectionProfile?>

  val connected: Flow<Boolean>
}
