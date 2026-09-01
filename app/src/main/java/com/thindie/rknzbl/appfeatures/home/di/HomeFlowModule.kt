package com.thindie.rknzbl.appfeatures.home.di

import com.thindie.rknzbl.feature.home.data.ConnectionProfileRepositoryImpl
import com.thindie.rknzbl.feature.home.domain.ConnectionProfileRepository
import com.v2ray.ang.runtime.KeyValueStorage

/**
 * One flow == one module.
 *
 * Owns the feature-specific repositories for the
 * [com.thindie.rknzbl.appfeatures.home.HomeFlow]. Created lazily by
 * [com.thindie.rknzbl.application.di.ApplicationScope.inject] and injected into the flow,
 * so the flow itself only needs the [com.thindie.engine.core.Router].
 *
 * Global repositories (e.g. [com.thindie.rknzbl.feature.settings.domain.SettingsRepository])
 * live in [com.thindie.rknzbl.application.di.ApplicationScope] and are injected separately.
 */
class HomeFlowModule(storage: KeyValueStorage) {
  val repository: ConnectionProfileRepository =
    ConnectionProfileRepositoryImpl(userName = "", password = "", url = "", storage = storage)
}
