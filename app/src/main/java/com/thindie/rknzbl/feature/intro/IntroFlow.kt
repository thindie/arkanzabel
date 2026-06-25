package com.thindie.rknzbl.feature.intro

import android.content.Context
import android.net.VpnService
import com.thindie.rknzbl.engine.Route
import com.thindie.rknzbl.engine.Router
import com.thindie.rknzbl.engine.ScreenFlow

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
