package com.thindie.rknzbl.appfeatures.home.ui

import android.content.Context
import com.thindie.engine.core.RouteFactory
import com.thindie.engine.core.WorkState
import com.thindie.engine.core.sub
import com.thindie.engine.core.transition
import com.thindie.rknzbl.feature.home.domain.ConnectionProfileRepository
import com.v2ray.ang.runtime.KeyValueStorage
import com.v2ray.ang.runtime.V2RayServiceManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Factory: creates the home-tab route for the new bottom-nav design.
 */
@Suppress("FunctionName")
internal fun HomeRoute(
  appContext: Context,
  repository: ConnectionProfileRepository,
) = RouteFactory.create(
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
            V2RayServiceManager.stopVService(appContext)
            s.copy(workState = WorkState.Idle, connectedProfile = null)
          }

          else -> {
            val guid = KeyValueStorage.getSelectServer()
            if (guid != null) {
              try {
                V2RayServiceManager.startVService(context = appContext, guid = guid)
                s.copy(workState = WorkState.Running)
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
          val connected = V2RayServiceManager.isRunning()
          val serverName = if (connected) V2RayServiceManager.getRunningServerName() else ""
          emit(connected to serverName)
          delay(2000L)
        }
      }

    scope.sub(connectionFlow).transition { state, pair ->
      when {
        pair.first -> {
          val guid = KeyValueStorage.getSelectServer()
          val profile = guid?.let { KeyValueStorage.decodeServerConfig(it) }
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
