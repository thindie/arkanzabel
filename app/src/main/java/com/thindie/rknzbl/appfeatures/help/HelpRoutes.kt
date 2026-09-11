package com.thindie.rknzbl.appfeatures.help

import com.thindie.engine.core.RouteFactory
import com.thindie.rknzbl.appfeatures.help.faq.helpFaq
import com.thindie.rknzbl.appfeatures.help.license.licenses
import com.thindie.rknzbl.appfeatures.help.vpnsetup.VpnSetupCommand
import com.thindie.rknzbl.appfeatures.help.vpnsetup.VpnSetupScreenContent
import com.thindie.rknzbl.appfeatures.help.vpnsetup.VpnSetupState

fun HelpFlow.helpHub() =
  RouteFactory.create(
    initialState = HelpState(),
    execute = { c: HelpCommand, s: HelpState ->
      when (c) {
        is HelpCommand.Back -> {
          back()
          null
        }
        is HelpCommand.OpenFaq -> {
          go(helpFaq())
          null
        }
        is HelpCommand.OpenLicenses -> {
          go(licenses())
          null
        }
        is HelpCommand.OpenVpnSetup -> {
          go(helpVpnSetup())
          null
        }
      }
    },
    id = "HelpFlow-helpHub",
    routeContent = ::HelpScreenContent,
  )

fun HelpFlow.helpVpnSetup() =
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
    id = "HelpFlow-helpVpnSetup",
    routeContent = ::VpnSetupScreenContent,
  )
