package com.thindie.rknzbl.appfeatures.settings.di

import com.thindie.rknzbl.appfeatures.settings.domain.SettingsRepository
import com.thindie.rknzbl.feature.home.domain.ConnectionProfileRepository

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
  val updateLocale: (String) -> Unit,
)
