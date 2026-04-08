package com.thindie.rknzbl.feature.home.data.di

import com.thindie.rknzbl.feature.home.data.ConnectionProfileRepositoryImpl
import com.thindie.rknzbl.feature.home.domain.ConnectionProfileRepository
import com.v2ray.ang.runtime.KeyValueStorage

class HomeFlowModule(
  private val userName: String,
  private val password: String,
  private val baseUrl: String,
  private val storage: KeyValueStorage,
) {
  private val _repository = ConnectionProfileRepositoryImpl(
    userName = userName, password = password, url = baseUrl, storage = storage,
  )

  val repository: ConnectionProfileRepository get() = _repository
}
