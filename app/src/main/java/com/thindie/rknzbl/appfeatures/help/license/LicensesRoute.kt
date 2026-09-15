package com.thindie.rknzbl.appfeatures.help.license

import com.thindie.engine.core.RouteFactory
import com.thindie.rknzbl.appfeatures.help.HelpFlow

fun HelpFlow.licenses() =
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
    id = "Help-licenses",
    routeContent = ::LicensesScreenContent,
  )
