package com.thindie.rknzbl.feature.home.ui.select

import com.thindie.engine.core.RouteFactory
import com.thindie.rknzbl.feature.home.HomeFlow
import com.thindie.rknzbl.feature.home.domain.ConnectionProfileRepository
import com.thindie.rknzbl.feature.home.ui.newprofiles.newProfiles
import com.thindie.rknzbl.feature.settings.domain.SettingsRepository
import com.thindie.rknzbl.feature.settings.ui.settings

fun HomeFlow.select(
  settingsRepository: SettingsRepository,
  connectionProfileRepository: ConnectionProfileRepository,
) = RouteFactory.create(
  initialState = ScreenState(),
  execute = { c: ScreenCommand, s: ScreenState ->
    when (c) {
      ScreenCommand.Home -> {
        startStoredProfilesFlow { }
        null
      }
      ScreenCommand.New -> {
        go(newProfiles())
        null
      }
      ScreenCommand.Settings -> {
        go(settings(settingsRepository, connectionProfileRepository))
        null
      }
      ScreenCommand.PerAppProxy -> {
        startPerAppProxyFlow()
        null
      }
      ScreenCommand.Back -> {
        finish(Unit)
        null
      }
      ScreenCommand.FetchAutoSaved -> {
        repository.fetchAutoSaved()
        s
      }
      ScreenCommand.DismissAutoSaved -> {
        repository.markAutoSavedSeen()
        s
      }
    }
  },
  initialCommand = { ScreenCommand.FetchAutoSaved },
  stateSink = ::selectStateSink,
  id = "HomeFlow-select",
  routeContent = ::HomeSelectScreen,
)
