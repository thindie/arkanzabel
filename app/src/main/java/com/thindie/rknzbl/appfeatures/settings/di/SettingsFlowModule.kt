package com.thindie.rknzbl.appfeatures.settings.di

import com.thindie.rknzbl.feature.settings.data.SettingsRepositoryImpl
import com.thindie.rknzbl.feature.settings.domain.SettingsRepository
import com.v2ray.ang.runtime.KeyValueStorage

/**
 * One flow == one module.
 *
 * Owns the feature-specific repositories for the
 * [com.thindie.rknzbl.appfeatures.settings.SettingsFlow]. Created lazily by
 * [com.thindie.rknzbl.application.di.ApplicationScope.inject] and injected into the flow,
 * so the flow itself only needs the [com.thindie.engine.core.Router].
 */
class SettingsFlowModule(storage: KeyValueStorage) {
  val repository: SettingsRepository = SettingsRepositoryImpl(storage = storage)
}
