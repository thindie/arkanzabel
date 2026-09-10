package com.thindie.rknzbl.appfeatures.intro

import android.content.Context
import android.net.VpnService
import com.thindie.engine.core.Route
import com.thindie.engine.core.Router
import com.thindie.engine.core.ScreenFlow

class IntroFlow(
  private val router: Router,
  val hasPushPermission: Boolean,
  val appContext: Context,
) : ScreenFlow<Route, IntroFlow.Result>(router) {
  enum class Result {
    Success,
  }

  internal fun hasVpnPermission(): Boolean = VpnService.prepare(appContext) == null

  override fun start() {
    go(main())
  }
}
