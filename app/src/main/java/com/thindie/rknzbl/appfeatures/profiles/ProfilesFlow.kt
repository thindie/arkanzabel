package com.thindie.rknzbl.appfeatures.profiles

import com.thindie.engine.core.Route
import com.thindie.engine.core.Router
import com.thindie.engine.core.ScreenFlow
import com.thindie.rknzbl.appfeatures.profiles.di.ProfilesFlowModule
import com.thindie.rknzbl.appfeatures.profiles.ui.ProfilesRoute

/**
 * Profiles feature flow.
 *
 * Takes only the [Router]; the [ProfilesFlowModule] is injected by
 * [com.thindie.rknzbl.application.di.ApplicationScope.inject].
 */
class ProfilesFlow(
  val router: Router,
) : ScreenFlow<Route, Unit>(router) {
  lateinit var flowModule: ProfilesFlowModule
    internal set

  override fun start() {
    go(ProfilesRoute(flowModule.connectionProfileRepository, flowModule.globalJobManager))
  }

  fun switch() {
    router.replaceTop(
      ProfilesRoute(flowModule.connectionProfileRepository, flowModule.globalJobManager),
    )
  }
}
