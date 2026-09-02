package com.thindie.rknzbl.appfeatures.home.ui

import com.thindie.engine.core.RouteFactory
import com.thindie.engine.core.WorkState
import com.thindie.engine.core.sub
import com.thindie.engine.core.transition
import com.thindie.rknzbl.feature.home.domain.ConnectionProfileRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

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

        ScreenCommand.ToggleConnect -> {
          when (s.workState) {
            is WorkState.Running -> {
              repository.disconnect()
              s.copy(workState = WorkState.Idle, connectedProfile = null)
            }

            else -> {
              val profile = repository.activeProfile()
              if (profile != null) {
                try {
                  repository.connect(profile.subscriptionId)
                  s.copy(workState = WorkState.Running, connectedProfile = profile)
                } catch (e: Exception) {
                  s.copy(
                    workState = WorkState.Error(e.message ?: "Failed to connect"),
                  )
                }
              } else {
                s.copy(workState = WorkState.Error("No server selected"))
              }
            }
          }
        }
      }
    },
    stateSink = { scope ->
      // Connection state flow (polls every 2 seconds)
      val connectionFlow: Flow<Pair<Boolean, String>> =
        flow {
          while (true) {
            val connected = repository.isConnected()
            val serverName = if (connected) repository.getConnectedServerName() else ""
            emit(connected to serverName)
            delay(2000L)
          }
        }

      scope.sub(connectionFlow).transition { state, pair ->
        when {
          pair.first -> {
            val profile = repository.activeProfile()
            state.copy(
              workState = WorkState.Running,
              connectedProfile = profile,
            )
          }

          state.workState is WorkState.Running -> state // still connecting
          else -> state.copy(workState = WorkState.Idle, connectedProfile = null)
        }
      }
    },
    routeContent = ::HomeScreenContent,
  )
