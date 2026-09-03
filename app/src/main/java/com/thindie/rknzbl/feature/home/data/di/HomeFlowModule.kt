package com.thindie.rknzbl.feature.home.data.di

import android.content.Context
import com.thindie.rknzbl.application.ProfilePingManager
import com.thindie.rknzbl.feature.home.data.ConnectionProfileRepositoryImpl
import com.thindie.rknzbl.feature.home.domain.ConnectionProfileRepository
import com.v2ray.ang.runtime.KeyValueStorage

/**
 * Legacy module for the classic home feature (HomeSelect hub).
 *
 * @deprecated New design uses [com.thindie.rknzbl.appfeatures.home.di.HomeFlowModule] with the shared repository from ApplicationScope.
 */
@Deprecated(message = "Use the shared ConnectionProfileRepository from ApplicationScope", level = DeprecationLevel.WARNING)
class HomeFlowModule(
  val appContext: Context,
  val pingManager: ProfilePingManager,
  private val userName: String,
  private val password: String,
  private val baseUrl: String,
  private val storage: KeyValueStorage,
) {
  private val _repository =
    ConnectionProfileRepositoryImpl(
      appContext = appContext,
      pingManager = pingManager,
      userName = userName,
      password = password,
      url = baseUrl,
      storage = storage,
    )

  val repository: ConnectionProfileRepository get() = _repository
}
