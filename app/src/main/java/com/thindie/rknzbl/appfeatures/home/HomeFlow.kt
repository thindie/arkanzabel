package com.thindie.rknzbl.appfeatures.home

import com.thindie.engine.core.Route
import com.thindie.engine.core.Router
import com.thindie.engine.core.ScreenFlow
import com.thindie.rknzbl.appfeatures.home.di.HomeFlowModule
import com.thindie.rknzbl.appfeatures.home.ui.HomeRoute

class HomeFlow(val router: Router) : ScreenFlow<Route, Unit>(router) {
  lateinit var flowModule: HomeFlowModule
    internal set

  override fun start() {
    go(
      HomeRoute(
        flowModule.connectionProfileRepository,
        flowModule.globalJobManager,
        flowModule.context,
        flowModule.appVersionResolver,
      ),
    )
  }

  fun switch() {
    router.replaceTop(
      HomeRoute(
        flowModule.connectionProfileRepository,
        flowModule.globalJobManager,
        flowModule.context,
        flowModule.appVersionResolver,
      ),
    )
  }
}
