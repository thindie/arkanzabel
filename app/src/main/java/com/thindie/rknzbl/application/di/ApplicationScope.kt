package com.thindie.rknzbl.application.di

import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import com.thindie.rknzbl.appfeatures.home.HomeFlow
import com.thindie.rknzbl.appfeatures.home.di.HomeFlowModule
import com.thindie.rknzbl.appfeatures.profiles.ProfilesFlow
import com.thindie.rknzbl.appfeatures.profiles.di.ProfilesFlowModule
import com.thindie.rknzbl.appfeatures.settings.SettingsFlow
import com.thindie.rknzbl.appfeatures.settings.data.SettingsRepositoryImpl
import com.thindie.rknzbl.appfeatures.settings.di.SettingsFlowModule
import com.thindie.rknzbl.appfeatures.settings.domain.SettingsRepository
import com.thindie.rknzbl.application.Application
import com.thindie.rknzbl.application.ProfilePingManager
import com.thindie.rknzbl.feature.home.data.ConnectionProfileRepositoryImpl
import com.thindie.rknzbl.feature.home.domain.ConnectionProfileRepository
import com.v2ray.ang.runtime.KeyValueStorage
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import com.thindie.rknzbl.feature.settings.data.SettingsRepositoryImpl as LegacySettingsRepositoryImpl
import com.thindie.rknzbl.feature.settings.domain.SettingsRepository as LegacySettingsRepository

/**
 * Root dependency graph.
 *
 * One flow == one module. Feature-specific modules ([HomeFlowModule], [ProfilesFlowModule],
 * [SettingsFlowModule]) are created once here and injected into flows via [inject].
 * Flows only need the Router; repositories come from this scope.
 *
 * Two settings repositories coexist during migration: [LegacySettingsRepository] for legacy
 * `feature/` screens and [SettingsRepositoryImpl] for new-design `appfeatures/` flows —
 * both read/write the same storage.
 */
class ApplicationScope private constructor(application: Application) {
  val coroutineScope =
    CoroutineScope(
      SupervisorJob() + Dispatchers.Default +
        CoroutineExceptionHandler { _, e -> "${e.message}" },
    )

  val pingManager = ProfilePingManager(application, coroutineScope)

  val connectionProfileRepository: ConnectionProfileRepository =
    ConnectionProfileRepositoryImpl(
      userName = "",
      password = "",
      url = "",
      storage = KeyValueStorage,
    )

  val settingsRepository: SettingsRepository = SettingsRepositoryImpl(storage = KeyValueStorage)

  private val legacySettingsRepositoryImpl = LegacySettingsRepositoryImpl(storage = KeyValueStorage)
  val settingsRepositoryLegacy: LegacySettingsRepository get() = legacySettingsRepositoryImpl

  private val updateLocaleFn: (String) -> Unit = { code ->
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val localeManager = application.getSystemService(LocaleManager::class.java)
      localeManager.applicationLocales = LocaleList.forLanguageTags(code)
    }
  }

  // Flow modules — created once, shared across flow instances.
  val homeFlowModule =
    HomeFlowModule(
      connectionProfileRepository = connectionProfileRepository,
      settingsRepository = legacySettingsRepositoryImpl,
      appContext = application,
    )

  val profilesFlowModule =
    ProfilesFlowModule(
      connectionProfileRepository = connectionProfileRepository,
    )

  val settingsFlowModule =
    SettingsFlowModule(
      settingsRepository = settingsRepository,
      connectionsProfileRepository = connectionProfileRepository,
      updateLocale = updateLocaleFn,
    )

  fun useNewDesignFeature(): Boolean {
    return (settingsRepository as SettingsRepositoryImpl).getUseNewDesignSync()
  }

  fun inject(homeFlow: HomeFlow) {
    homeFlow.flowModule = homeFlowModule
  }

  fun inject(settingsFlow: SettingsFlow) {
    settingsFlow.flowModule = settingsFlowModule
  }

  fun inject(profilesFlow: ProfilesFlow) {
    profilesFlow.flowModule = profilesFlowModule
  }

  fun destroy() {
    coroutineScope.cancel()
  }

  companion object {
    fun configure(application: Application): ApplicationScope {
      return ApplicationScope(application)
    }
  }
}
