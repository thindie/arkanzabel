package com.thindie.rknzbl.appfeatures.home.ui

import com.thindie.engine.core.RouteFactory
import com.thindie.engine.core.sub
import com.thindie.engine.core.transition
import com.thindie.rknzbl.appfeatures.home.HomeSection
import com.thindie.rknzbl.application.work.GlobalJobManager
import com.thindie.rknzbl.application.work.GlobalJobManager.Companion.CONNECT_KEY
import com.thindie.rknzbl.application.work.GlobalJobManager.Companion.FETCH_KEY_HOME
import com.thindie.rknzbl.feature.home.domain.ConnectionProfileRepository

/**
 * Factory: creates the home-tab route for the new bottom-nav design.
 */
@Suppress("FunctionName", "MagicNumber")
internal fun HomeRoute(
  repository: ConnectionProfileRepository,
  globalJobManager: GlobalJobManager,
) = RouteFactory.create(
  id = "HomeFlow-home",
  initialState = ScreenState(),
  execute = { c: ScreenCommand, s: ScreenState ->
    when (c) {
      ScreenCommand.ToggleConnect -> {
        globalJobManager.launchGlobal(FETCH_KEY_HOME) { repository.fetch(false) }
        null
      }
    }
  },
  section = HomeSection.Home,
  stateSink = { screenScope ->
    // Best measured profile — connect target shown while not connected
    screenScope.sub(repository.lastMeasured).transition(
      action = { _, _, profile ->
        if (profile == null) return@transition
        globalJobManager.launchGlobal(CONNECT_KEY, { repository.connect(profile) })
      },
    )

    screenScope.sub(repository.connected).transition { state, connected ->
      state.copy(connectedProfile = connected)
    }

    screenScope.sub(globalJobManager.isRunning(FETCH_KEY_HOME)).transition { state, running ->
      state.copy(profilesLoading = running)
    }

    screenScope.sub(globalJobManager.isRunning(CONNECT_KEY)).transition { state, running ->
      state.copy(serviceConnection = running)
    }
  },
  routeContent = ::HomeScreenContent,
)
