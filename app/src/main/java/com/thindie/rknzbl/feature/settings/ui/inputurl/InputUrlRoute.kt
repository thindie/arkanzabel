package com.thindie.rknzbl.feature.settings.ui.inputurl

import com.thindie.engine.core.RouteFactory
import com.thindie.rknzbl.feature.home.HomeFlow

fun HomeFlow.createInputUrl() =
  RouteFactory.create(
    initialState = InputUrlState(),
    execute = { c: InputUrlCommand, s: InputUrlState ->
      when (c) {
        is InputUrlCommand.SetUrl -> s.copy(url = c.url)
        InputUrlCommand.Back -> {
          back()
          settingsRepository.setCustomSourceEnabled(false)
          null
        }
        InputUrlCommand.Done -> {
          settingsRepository.setCustomSourceUrl(s.url)
          back()
          null
        }
      }
    },
    id = "InputUrlRoute",
    routeContent = ::InputUrlScreenContent,
  )
