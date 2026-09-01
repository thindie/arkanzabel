package com.thindie.rknzbl.application.di

import com.thindie.rknzbl.appfeatures.home.HomeFlow
import com.thindie.rknzbl.appfeatures.home.di.HomeFlowModule
import com.thindie.rknzbl.appfeatures.profiles.ProfilesFlow
import com.thindie.rknzbl.appfeatures.profiles.di.ProfilesFlowModule
import com.thindie.rknzbl.appfeatures.settings.SettingsFlow
import com.thindie.rknzbl.appfeatures.settings.di.SettingsFlowModule
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
  private var settingsFlowModule: SettingsFlowModule? = null
  private var profilesFlowModule: ProfilesFlowModule? = null
  private lateinit var storage: KeyValueStorage

  fun inject(homeFlow: HomeFlow) {
    homeFlow.flowModule = homeFlowModule
      ?: HomeFlowModule(storage = storage)
        .also { homeFlowModule = it }

    homeFlow.settingsRepository = settingsRepositoryImpl
      ?: SettingsRepositoryImpl(storage = storage)
        .also { settingsRepositoryImpl = it }
  }

  fun inject(settingsFlow: SettingsFlow) {
    settingsFlow.flowModule = settingsFlowModule
      ?: SettingsFlowModule(storage = storage)
        .also { settingsFlowModule = it }

    settingsFlow.settingsRepository = settingsRepositoryImpl
      ?: SettingsRepositoryImpl(storage = storage)
        .also { settingsRepositoryImpl = it }
  }

  fun inject(profilesFlow: ProfilesFlow) {
    profilesFlow.flowModule = profilesFlowModule
      ?: ProfilesFlowModule(storage = storage)
        .also { profilesFlowModule = it }
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
