package com.thindie.rknzbl.feature.home.domain

import com.v2ray.ang.dto.ConnectionProfile

interface ConnectionProfileRepository {
  suspend fun read(): List<ConnectionProfile>

  suspend fun save(guid: String)

  suspend fun delete(profile: ConnectionProfile)
}
