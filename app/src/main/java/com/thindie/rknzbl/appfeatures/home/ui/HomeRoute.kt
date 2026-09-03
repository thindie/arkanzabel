package com.thindie.rknzbl.appfeatures.home.ui

import com.thindie.engine.core.RouteFactory
import com.thindie.engine.core.WorkState
import com.thindie.engine.core.sub
import com.thindie.engine.core.transition
import com.thindie.rknzbl.appfeatures.home.HomeSection
import com.thindie.rknzbl.feature.home.domain.ConnectionProfileRepository

private const val CONNECT_TIMEOUT_MS = 15_000L

/**
 * Factory: creates the home-tab route for the new bottom-nav design.
 */
@Suppress("FunctionName", "MagicNumber")
internal fun HomeRoute(repository: ConnectionProfileRepository) =
  RouteFactory.create(
    id = "HomeFlow-home",
    initialState = ScreenState(),
    execute = { c: ScreenCommand, s: ScreenState ->
      when (c) {
        ScreenCommand.LoadProfiles -> {
          repository.fetch()
          null
        }

        ScreenCommand.Home -> null
        ScreenCommand.New -> null
        ScreenCommand.PerAppProxy -> null

        ScreenCommand.ToggleConnect -> {
          when (s.workState) {
            is WorkState.Running -> {
              repository.disconnect()
              s.copy(workState = WorkState.Idle, connectedProfile = null)
            }

            else -> {
              // Connect to best measured profile or active if none measured yet.
              // Target must be stored locally before the service can run it,
              // so pass the whole profile instead of a raw GUID.
              val target = s.connectedProfile ?: repository.activeProfile()
              if (target != null) {
                // Profile is confirmed by the service state flow; spinner shows meanwhile
                repository.connect(target)
                s.copy(
                  workState = WorkState.Running,
                  connectedProfile = null,
                  connectingSince = System.currentTimeMillis(),
                )
              } else {
                s.copy(workState = WorkState.Error("No server selected"))
              }
            }
          }
        }
      }
    },
    initialCommand = {
      ScreenCommand.LoadProfiles as ScreenCommand
    },
    section = HomeSection.Home,
    stateSink = { screenScope ->
      // Best measured profile — connect target shown while not connected
      screenScope.sub(repository.measured).transition { state, bestProfile ->
        if (bestProfile != null && state.workState !is WorkState.Running) {
          state.copy(connectedProfile = bestProfile)
        } else {
          state
        }
      }

      // Service running state (repository polls every 2 seconds)
      screenScope.sub(repository.connected).transition { state, connected ->
        when {
          connected ->
            state.copy(
              workState = WorkState.Running,
              connectedProfile = repository.activeProfile() ?: state.connectedProfile,
            )

          !connected && state.workState is WorkState.Running && state.connectingSince == null -> {
            // Service stopped while running and we don't track an active connect: treat as idle
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
              state // still connecting, wait for confirmation or timeout
            }
          }

          else -> {
            // Not running: keep measured target for display, reset work state only
            if (state.workState is WorkState.Idle) {
              state
            } else {
              state.copy(workState = WorkState.Idle)
            }
          }
        }
      }
    },
    routeContent = ::HomeScreenContent,
  )
