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
}
