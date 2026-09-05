package com.thindie.rknzbl.appfeatures.logs

import com.thindie.engine.core.Route
import com.thindie.engine.core.Router
import com.thindie.engine.core.ScreenFlow
import com.thindie.rknzbl.appfeatures.logs.ui.LogsRoute

class LogsFlow(val router: Router) : ScreenFlow<Route, Unit>(router) {
  override fun start() {
    go(LogsRoute())
  }

  fun switch() {
    router.replaceTop(LogsRoute())
  }
}
