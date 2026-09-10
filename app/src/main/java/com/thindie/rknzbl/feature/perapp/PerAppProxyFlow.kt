package com.thindie.rknzbl.feature.perapp

import android.content.Context
import com.thindie.engine.core.Route
import com.thindie.engine.core.Router
import com.thindie.engine.core.ScreenFlow

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
