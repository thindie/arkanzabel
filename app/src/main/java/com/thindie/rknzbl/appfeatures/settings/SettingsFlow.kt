package com.thindie.rknzbl.appfeatures.settings

import com.thindie.engine.core.Route
import com.thindie.engine.core.Router
import com.thindie.engine.core.ScreenFlow
import com.thindie.rknzbl.appfeatures.settings.di.SettingsFlowModule
import com.thindie.rknzbl.feature.settings.domain.SettingsRepository

/**
 * Settings feature flow.
 *
 * Takes only the [Router]; the [SettingsFlowModule] (feature-specific repositories) and the
 * global [SettingsRepository] are injected by
 * [com.thindie.rknzbl.application.di.ApplicationScope.inject].
 *
 * Full flow logic (routes/screens) is added as the feature is migrated in.
 */
class SettingsFlow(
  val router: Router,
) : ScreenFlow<Route, Unit>(router) {
  lateinit var flowModule: SettingsFlowModule
    internal set

  lateinit var settingsRepository: SettingsRepository
    internal set

  override fun start() {}
}
