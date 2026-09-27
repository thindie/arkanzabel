package com.thindie.rknzbl.domain

import com.thindie.engine.core.WorkState
import com.v2ray.ang.dto.ConnectionProfile
import kotlinx.coroutines.flow.Flow

interface ConnectionProfileRepository {
  suspend fun read(): List<ConnectionProfile>

  suspend fun save(guid: String): Boolean

  suspend fun delete(profile: ConnectionProfile)

  suspend fun saveAuto(guid: String)

  fun invalidateCaches()

  fun invalidateStoredCache()

  // VPN service operations
  suspend fun connect(profile: ConnectionProfile)

  suspend fun disconnect()

  val vpnState: Flow<WorkState>

  suspend fun fetch(force: Boolean)

  fun takeConnectIntent(): Boolean

  /** Mark a profile as used for next-best selection (survives route recreation). */
  fun markProfileUsed(profile: ConnectionProfile)

  suspend fun measureStaged(): ConnectionProfile?

  val received: Flow<List<ConnectionProfile>?>
  val stored: Flow<List<ConnectionProfile>?>

  val lastMeasured: Flow<ConnectionProfile?>

  val connected: Flow<ConnectionProfile?>

  fun isLocalStorage(): Boolean
}
