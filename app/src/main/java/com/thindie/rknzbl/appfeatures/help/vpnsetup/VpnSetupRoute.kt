package com.thindie.rknzbl.appfeatures.help.vpnsetup

import com.thindie.engine.core.RouteFactory
import com.thindie.rknzbl.appfeatures.settings.SettingsFlow

fun SettingsFlow.vpnSetup() =
  RouteFactory.create(
    initialState = VpnSetupState(),
    execute = { c: VpnSetupCommand, s: VpnSetupState ->
      when (c) {
        is VpnSetupCommand.Back -> {
          back()
          null
        }
      }
    },
    id = "SettingsFlow-vpnsetup",
    routeContent = ::VpnSetupScreenContent,
  )
