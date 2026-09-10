package com.thindie.rknzbl.appfeatures.settings

import com.thindie.engine.core.Route
import com.thindie.engine.core.Router
import com.thindie.engine.core.ScreenFlow
import com.thindie.rknzbl.appfeatures.settings.di.SettingsFlowModule
import com.thindie.rknzbl.appfeatures.settings.ui.settings

class SettingsFlow(
  val router: Router,
) : ScreenFlow<Route, Unit>(router) {
  lateinit var flowModule: SettingsFlowModule
    internal set

  override fun start() {
    go(settingsRoute())
  }

  fun switch() {
    router.replaceTop(settingsRoute())
  }

  private fun settingsRoute() =
    settings(
      repository = flowModule.settingsRepository,
      connectionProfileRepository = flowModule.connectionsProfileRepository,
    )
}
