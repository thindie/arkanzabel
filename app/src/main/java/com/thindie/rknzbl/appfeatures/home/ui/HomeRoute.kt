package com.thindie.rknzbl.appfeatures.home.ui

import com.thindie.engine.core.RouteFactory
import com.thindie.engine.core.sub
import com.thindie.engine.core.transition
import com.thindie.rknzbl.feature.home.domain.ConnectionProfileRepository

/**
 * Factory: creates the home-tab route for the new bottom-nav design.
 */
@Suppress("FunctionName")
internal fun HomeRoute(repository: ConnectionProfileRepository) =
  RouteFactory.create(
    id = "HomeFlow-home",
    initialState = ScreenState(),
    execute = { c: ScreenCommand, s: ScreenState ->
      when (c) {
        ScreenCommand.Home -> null
        ScreenCommand.New -> null
        ScreenCommand.PerAppProxy -> null
        ScreenCommand.DismissAutoSaved -> {
          repository.markAutoSavedSeen()
          s
        }
      }
    },
    initialCommand = RouteFactory.InitialCommand { ScreenCommand.DismissAutoSaved },
    stateSink = { scope ->
      scope.sub(repository.autoSaved()).transition { _, autosaved ->
        if (autosaved == null) ScreenState(null) else ScreenState(autosaved)
      }
    },
    routeContent = ::HomeScreenContent,
  )
