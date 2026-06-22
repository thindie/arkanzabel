package com.thindie.rknzbl.feature.settings.data.di

import com.thindie.rknzbl.feature.settings.data.SettingsRepositoryImpl
import com.thindie.rknzbl.feature.settings.domain.SettingsRepository
import com.v2ray.ang.runtime.KeyValueStorage

class SettingsFlowModule(
  private val storage: KeyValueStorage,
) {
  private val _repository =
    SettingsRepositoryImpl(
      storage = storage,
    )

  val repository: SettingsRepository get() = _repository
}
