package com.thindie.rknzbl.appfeatures.settings.ui.licenses

import com.thindie.engine.core.RouteFactory
import com.thindie.rknzbl.appfeatures.settings.SettingsFlow

fun SettingsFlow.licenses() =
  RouteFactory.create(
    initialState = LicensesState(),
    execute = { c: LicensesCommand, s: LicensesState ->
      when (c) {
        is LicensesCommand.Back -> {
          back()
          null
        }
      }
    },
    id = "SettingsFlow-licenses",
    routeContent = ::LicensesScreenContent,
  )
