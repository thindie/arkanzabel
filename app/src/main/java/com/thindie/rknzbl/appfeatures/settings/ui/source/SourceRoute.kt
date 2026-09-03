package com.thindie.rknzbl.appfeatures.settings.ui.source

import com.thindie.engine.core.RouteFactory
import com.thindie.rknzbl.appfeatures.settings.SettingsFlow

fun SettingsFlow.source() =
  RouteFactory.create(
    initialState = SourceState(),
    execute = { c: SourceCommand, s: SourceState ->
      when (c) {
        is SourceCommand.Back -> {
          back()
          null
        }

        is SourceCommand.SelectPreset -> {
          flowModule.settingsRepository.setCustomSourceUrl(c.url)
          flowModule.settingsRepository.setCustomSourceEnabled(true)
          back()
          null
        }

        is SourceCommand.SetCustomUrl -> s.copy(customUrlInput = c.url)

        SourceCommand.ClearSource -> {
          flowModule.settingsRepository.setCustomSourceEnabled(false)
          back()
          null
        }
      }
    },
    stateSink = { screenScope -> sourceStateSink(screenScope, flowModule.settingsRepository) },
    id = "SettingsFlow-source",
    routeContent = ::SourceScreenContent,
  )
