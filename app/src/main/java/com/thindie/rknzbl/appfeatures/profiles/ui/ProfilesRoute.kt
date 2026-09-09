package com.thindie.rknzbl.appfeatures.profiles.ui

import com.thindie.engine.core.RouteFactory
import com.thindie.engine.core.ScreenScope
import com.thindie.engine.core.sub
import com.thindie.engine.core.transition
import com.thindie.rknzbl.appfeatures.home.HomeSection
import com.thindie.rknzbl.appfeatures.profiles.ProfilesFlow
import com.thindie.rknzbl.appfeatures.profiles.deletesaved.deleteSavedProfiles
import com.thindie.rknzbl.application.work.GlobalJobManager.Companion.CONNECT_KEY
import com.thindie.rknzbl.application.work.GlobalJobManager.Companion.FETCH_KEY
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.withContext

@Suppress("MagicNumber")
internal fun ProfilesFlow.profiles() =
  RouteFactory.create(
    initialState = ScreenState(),
    execute = ::exec,
    stateSink = ::stateSink,
    routeContent = ::ProfilesScreenContent,
    initialCommand = { ScreenCommand.LoadProfiles },
    id = "ProfilesFlow-profiles",
    section = HomeSection.Profiles,
  )

internal suspend fun ProfilesFlow.exec(
  c: ScreenCommand,
  s: ScreenState,
): ScreenState? {
  val repository = flowModule.connectionProfileRepository
  val globalJobManager = flowModule.globalJobManager
  return when (c) {
    ScreenCommand.LoadProfiles -> {
      globalJobManager.launchGlobal(FETCH_KEY) { repository.fetch(false) }
      null
    }

    is ScreenCommand.SelectTab -> {
      val next = s.copy(selectedTab = c.index)
      if (c.index == 1) {
        // Load stored profiles on demand: the reactive `stored` flow only emits
        // after the local-storage cache has been populated by a read.
        val stored =
          withContext(Dispatchers.IO) {
            repository.invalidateStoredCache()
            repository.read()
          }
        next.copy(savedProfiles = stored)
      } else {
        next
      }
    }

    is ScreenCommand.ConnectProfile -> {
      globalJobManager.launchGlobal(CONNECT_KEY) { repository.connect(c.profile) }
      null
    }

    ScreenCommand.OpenDeleteSavedProfiles -> {
      go(deleteSavedProfiles())
      null
    }
  }
}

internal fun ProfilesFlow.stateSink(screenScope: ScreenScope<ScreenState, ScreenCommand>) {
  val repository = flowModule.connectionProfileRepository
  val globalJobManager = flowModule.globalJobManager

  // All profiles after fetch + ping
  screenScope.sub(repository.received.filterNotNull()).transition { state, allProfiles ->
    state.copy(profiles = allProfiles)
  }

  screenScope.sub(repository.stored.filterNotNull()).transition { state, saved ->
    state.copy(savedProfiles = saved)
  }

  // Best measured profile — connect target shown while not connected
  screenScope.sub(repository.lastMeasured).transition { state, bestProfile ->
    if (bestProfile != null && state.connectedProfile == null) {
      state.copy(connectedProfile = bestProfile)
    } else {
      state
    }
  }

  // Service running state
  screenScope.sub(repository.connected).transition { state, connected ->
    state.copy(connectedProfile = connected)
  }

  screenScope.sub(globalJobManager.isRunning(FETCH_KEY)).transition { state, running ->
    state.copy(profilesLoading = running)
  }

  screenScope.sub(globalJobManager.isRunning(CONNECT_KEY)).transition { state, running ->
    state.copy(serviceConnection = running)
  }
}
