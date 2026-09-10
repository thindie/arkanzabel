package com.thindie.rknzbl.appfeatures.settings.ui.faq

import com.thindie.engine.core.RouteFactory
import com.thindie.rknzbl.appfeatures.settings.SettingsFlow

fun SettingsFlow.faq() =
  RouteFactory.create(
    initialState = FaqState(),
    execute = { c: FaqCommand, s: FaqState ->
      when (c) {
        is FaqCommand.Back -> {
          back()
          null
        }
      }
    },
    id = "SettingsFlow-faq",
    routeContent = ::FaqScreenContent,
  )
