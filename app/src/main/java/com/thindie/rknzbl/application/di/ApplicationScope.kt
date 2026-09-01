package com.thindie.rknzbl.application.di

import com.thindie.rknzbl.appfeatures.home.HomeFlow
import com.thindie.rknzbl.appfeatures.home.di.HomeFlowModule
import com.thindie.rknzbl.feature.settings.data.SettingsRepositoryImpl
import com.thindie.rknzbl.feature.settings.domain.SettingsRepository
import com.v2ray.ang.runtime.KeyValueStorage

/**
 * Root dependency graph.
 *
 * One flow == one module. Feature-specific modules (e.g. [HomeFlowModule]) are created
 * lazily on first [inject] (or direct access) and cached, so a flow only needs the
 * [com.thindie.engine.core.Router] and gets its repositories injected.
 *
 * Global repositories (e.g. [SettingsRepository]) are shared across flows and also
 * injected here.
 */
class ApplicationScope {
  private var homeFlowModule: HomeFlowModule? = null
  private var settingsRepositoryImpl: SettingsRepository? = null
  private lateinit var storage: KeyValueStorage

  fun inject(homeFlow: HomeFlow) {
    homeFlow.flowModule = homeFlowModule
      ?: HomeFlowModule(storage = storage)
        .also { homeFlowModule = it }

    homeFlow.settingsRepository = settingsRepositoryImpl
      ?: SettingsRepositoryImpl(storage = storage)
        .also { settingsRepositoryImpl = it }
  }

  val homeModule: HomeFlowModule
    get() =
      homeFlowModule
        ?: HomeFlowModule(storage = storage)
          .also { homeFlowModule = it }

  val settingsRepository: SettingsRepository
    get() =
      settingsRepositoryImpl
        ?: SettingsRepositoryImpl(storage = storage)
          .also { settingsRepositoryImpl = it }

  companion object {
    fun configure(): ApplicationScope =
      ApplicationScope()
        .apply { storage = KeyValueStorage }
  }
}
