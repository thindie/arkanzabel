package com.thindie.rknzbl.application.di

import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import com.thindie.rknzbl.appfeatures.home.HomeFlow
import com.thindie.rknzbl.appfeatures.home.di.HomeFlowModule
import com.thindie.rknzbl.appfeatures.profiles.ProfilesFlow
import com.thindie.rknzbl.appfeatures.profiles.di.ProfilesFlowModule
import com.thindie.rknzbl.appfeatures.settings.SettingsFlow
import com.thindie.rknzbl.appfeatures.settings.data.PerAppProxyRepositoryImpl
import com.thindie.rknzbl.appfeatures.settings.data.SettingsRepositoryImpl
import com.thindie.rknzbl.appfeatures.settings.di.SettingsFlowModule
import com.thindie.rknzbl.appfeatures.settings.domain.PerAppProxyRepository
import com.thindie.rknzbl.appfeatures.settings.domain.SettingsRepository
import com.thindie.rknzbl.application.Application
import com.thindie.rknzbl.application.LogSinkProvider
import com.thindie.rknzbl.application.ProfilePingManager
import com.thindie.rknzbl.application.work.GlobalJobManager
import com.thindie.rknzbl.feature.home.data.ConnectionProfileRepositoryImpl
import com.thindie.rknzbl.feature.home.data.ProfileHttpGateway
import com.thindie.rknzbl.feature.home.data.ProfileHttpGatewayImpl
import com.thindie.rknzbl.feature.home.data.V2RayVpnServiceGateway
import com.thindie.rknzbl.feature.home.domain.ConnectionProfileRepository
import com.v2ray.ang.runtime.KeyValueStorage
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
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

  // App-level job manager: long-running work (measure/connect) survives screen navigation.
  val globalJobManager = GlobalJobManager(coroutineScope)

  private val profileHttpGateway: ProfileHttpGateway =
    ProfileHttpGatewayImpl(webDavUrl = "", userName = "", password = "")

  private val vpnServiceGateway = V2RayVpnServiceGateway()

  val connectionProfileRepository: ConnectionProfileRepository =
    ConnectionProfileRepositoryImpl(
      appContext = application,
      pingManager = pingManager,
      storage = KeyValueStorage,
      httpGateway = profileHttpGateway,
      vpnGateway = vpnServiceGateway,
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

  val homeFlowModule =
    HomeFlowModule(
      connectionProfileRepository = connectionProfileRepository,
      globalJobManager = globalJobManager,
    )

  val profilesFlowModule =
    ProfilesFlowModule(
      connectionProfileRepository = connectionProfileRepository,
      globalJobManager = globalJobManager,
    )

  private val perAppProxyRepository: PerAppProxyRepository =
    PerAppProxyRepositoryImpl(appContext = application, storage = KeyValueStorage)

  val settingsFlowModule =
    SettingsFlowModule(
      settingsRepository = settingsRepository,
      connectionsProfileRepository = connectionProfileRepository,
      perAppProxyRepository = perAppProxyRepository,
      updateLocale = updateLocaleFn,
    )

  fun useNewDesignFeature(): Flow<Boolean> {
    return (settingsRepository as SettingsRepositoryImpl).useNewDesign
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

  companion object {
    fun configure(application: Application): ApplicationScope {
      LogSinkProvider.init()
      return ApplicationScope(application)
    }
  }
}
