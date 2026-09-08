package com.thindie.rknzbl.appfeatures.profiles.ui

import com.thindie.engine.core.RouteFactory
import com.thindie.engine.core.sub
import com.thindie.engine.core.transition
import com.thindie.rknzbl.appfeatures.home.HomeSection
import com.thindie.rknzbl.application.work.GlobalJobManager
import com.thindie.rknzbl.application.work.GlobalJobManager.Companion.CONNECT_KEY
import com.thindie.rknzbl.application.work.GlobalJobManager.Companion.FETCH_KEY
import com.thindie.rknzbl.feature.home.domain.ConnectionProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.withContext

/**
 * Factory: creates the profiles-tab route for the new bottom-nav design.
 */
@Suppress("FunctionName", "MagicNumber")
internal fun ProfilesRoute(
  repository: ConnectionProfileRepository,
  globalJobManager: GlobalJobManager,
) = RouteFactory.create(
  id = "ProfilesFlow-profiles",
  initialState = ScreenState(),
  execute =
    suspend { c: ScreenCommand, s: ScreenState ->
      when (c) {
        is ScreenCommand.LoadProfiles -> {
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
      }
    },
  initialCommand = { ScreenCommand.LoadProfiles as ScreenCommand },
  section = HomeSection.Profiles,
  stateSink = { screenScope ->
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
  },
  routeContent = ::ProfilesScreenContent,
)
