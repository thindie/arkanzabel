package com.thindie.rknzbl.appfeatures.home.ui

import com.thindie.engine.core.RouteFactory
import com.thindie.engine.core.sub
import com.thindie.engine.core.transition
import com.thindie.rknzbl.feature.home.domain.ConnectionProfileRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import com.v2ray.ang.runtime.V2RayServiceManager

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
      // Connection state flow (polls every 2 seconds)
      val connectionFlow: Flow<Pair<Boolean, String>> = flow {
        while (true) {
          val connected = V2RayServiceManager.isRunning()
          val serverName = if (connected) V2RayServiceManager.getRunningServerName() else ""
          emit(connected to serverName)
          delay(2000L)
        }
      }

      scope.sub(connectionFlow).transition { state, pair ->
        state.copy(isConnected = pair.first, serverName = pair.second)
      }

      // Auto-saved profile from repository
      scope.sub(repository.autoSaved()).transition { state, autosaved ->
        state.copy(autoSaved = autosaved)
      }
    },
    routeContent = ::HomeScreenContent,
  )
