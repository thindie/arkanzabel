package com.thindie.rknzbl.appfeatures.profiles.ui

import com.thindie.engine.core.RouteFactory
import com.thindie.engine.core.WorkState
import com.thindie.engine.core.sub
import com.thindie.engine.core.transition
import com.thindie.rknzbl.appfeatures.home.HomeSection
import com.thindie.rknzbl.feature.home.domain.ConnectionProfileRepository

private const val CONNECT_TIMEOUT_MS = 15_000L

/**
 * Factory: creates the profiles-tab route for the new bottom-nav design.
 */
@Suppress("FunctionName", "MagicNumber")
internal fun ProfilesRoute(repository: ConnectionProfileRepository) =
  RouteFactory.create(
    id = "ProfilesFlow-profiles",
    initialState = ScreenState(),
    execute =
      suspend { c: ScreenCommand, s: ScreenState ->
        when (c) {
          is ScreenCommand.LoadProfiles -> {
            repository.fetch()
            null
          }

          is ScreenCommand.SelectTab -> s.copy(selectedTab = c.index)

          is ScreenCommand.ConnectProfile -> {
            if (s.workState !is WorkState.Running) {
              repository.connect(c.profile)
              s.copy(
                workState = WorkState.Running,
                connectedProfile = null,
                connectingSince = System.currentTimeMillis(),
              )
            } else {
              s
            }
          }

          is ScreenCommand.RefreshProfiles -> {
            repository.invalidateCaches()
            repository.fetch()
            null
          }
        }
      },
    initialCommand = { ScreenCommand.LoadProfiles as ScreenCommand },
    section = HomeSection.Profiles,
    stateSink = { screenScope ->
      // All profiles after fetch + ping
      screenScope.sub(repository.profiles).transition { state, allProfiles ->
        val saved = allProfiles.filter { repository.isSaved(it) }
        val main = allProfiles - saved.toSet()
        state.copy(profiles = main, savedProfiles = saved)
      }

      // Best measured profile — connect target shown while not connected
      screenScope.sub(repository.measured).transition { state, bestProfile ->
        if (bestProfile != null && state.workState !is WorkState.Running) {
          state.copy(connectedProfile = bestProfile)
        } else {
          state
        }
      }

      // Service running state
      screenScope.sub(repository.connected).transition { state, connected ->
        when {
          connected ->
            state.copy(
              workState = WorkState.Running,
              connectedProfile = repository.activeProfile() ?: state.connectedProfile,
            )

          !connected && state.workState is WorkState.Running && state.connectingSince == null -> {
            state.copy(workState = WorkState.Idle, connectedProfile = null)
          }

          !connected && state.workState is WorkState.Running -> {
            val since = state.connectingSince
            if (since != null && System.currentTimeMillis() - since > CONNECT_TIMEOUT_MS) {
              state.copy(
                workState = WorkState.Error("Failed to connect"),
                connectedProfile = null,
              )
            } else {
              state // still connecting
            }
          }

          else -> {
            if (state.workState is WorkState.Idle) {
              state
            } else {
              state.copy(workState = WorkState.Idle)
            }
          }
        }
      }
    },
    routeContent = ::ProfilesScreenContent,
  )
