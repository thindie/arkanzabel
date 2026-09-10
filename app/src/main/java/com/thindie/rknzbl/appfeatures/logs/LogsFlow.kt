package com.thindie.rknzbl.appfeatures.logs

import android.content.Context
import com.thindie.engine.core.Route
import com.thindie.engine.core.Router
import com.thindie.engine.core.ScreenFlow
import com.thindie.rknzbl.appfeatures.logs.ui.LogsRoute

class LogsFlow(val router: Router, private val appContext: Context) :
  ScreenFlow<Route, Unit>(router) {
  override fun start() {
    go(LogsRoute(appContext))
  }

  fun switch() {
    router.replaceTop(LogsRoute(appContext))
  }
}
