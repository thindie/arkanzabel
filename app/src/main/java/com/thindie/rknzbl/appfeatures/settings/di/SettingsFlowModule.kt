package com.thindie.rknzbl.appfeatures.settings.di

import com.thindie.rknzbl.appfeatures.settings.domain.PerAppProxyRepository
import com.thindie.rknzbl.domain.ConnectionProfileRepository
import com.thindie.rknzbl.domain.SettingsRepository

/**
 * One flow == one module.
 *
 * Owns the feature-specific dependencies for the [com.thindie.rknzbl.appfeatures.settings.SettingsFlow]:
 * settings repository, profile repository (for cache invalidation on storage-mode switch),
 * and the locale updater. Created once in [com.thindie.rknzbl.application.di.ApplicationScope].
 */
class SettingsFlowModule(
  val settingsRepository: SettingsRepository,
  val connectionsProfileRepository: ConnectionProfileRepository,
  val perAppProxyRepository: PerAppProxyRepository,
  val updateLocale: (String) -> Unit,
)
