package com.thindie.rknzbl.feature.perapp

import android.content.Context
import com.thindie.rknzbl.engine.Route
import com.thindie.rknzbl.engine.Router
import com.thindie.rknzbl.engine.ScreenFlow

enum class ProxyScopeMode {
  All,
  Selected,
}

class PerAppProxyFlow(
  private val router: Router,
  val appContext: Context,
) : ScreenFlow<Route, Unit>(router) {
  override fun start() {
    go(main())
  }
}
