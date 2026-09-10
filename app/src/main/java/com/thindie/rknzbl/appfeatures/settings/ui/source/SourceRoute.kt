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
          // Presence of the URL itself marks the custom source as active
          flowModule.settingsRepository.setCustomSourceUrl(c.url)
          back()
          null
        }

        is SourceCommand.SetCustomUrl -> s.copy(customUrlInput = c.url)

        SourceCommand.ClearSource -> {
          // Clearing the URL disables the custom source
          flowModule.settingsRepository.setCustomSourceUrl(null)
          back()
          null
        }
      }
    },
    stateSink = { screenScope -> sourceStateSink(screenScope, flowModule.settingsRepository) },
    id = "SettingsFlow-source",
    routeContent = ::SourceScreenContent,
  )
