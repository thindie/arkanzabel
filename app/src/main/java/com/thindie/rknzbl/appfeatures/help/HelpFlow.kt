package com.thindie.rknzbl.appfeatures.help

import com.thindie.engine.core.Route
import com.thindie.engine.core.Router
import com.thindie.engine.core.ScreenFlow

class HelpFlow(
  val router: Router,
) : ScreenFlow<Route, Unit>(router) {
  override fun start() {
    go(helpHub())
  }
}
