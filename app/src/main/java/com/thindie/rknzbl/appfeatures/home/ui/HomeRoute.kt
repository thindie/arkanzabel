package com.thindie.rknzbl.appfeatures.home.ui

import com.thindie.engine.core.RouteFactory
import com.thindie.engine.core.WorkState
import com.thindie.engine.core.sub
import com.thindie.engine.core.transition
import com.thindie.rknzbl.appfeatures.home.HomeSection
import com.thindie.rknzbl.application.work.GlobalJobManager
import com.thindie.rknzbl.application.work.GlobalJobManager.Companion.CONNECT_KEY
import com.thindie.rknzbl.application.work.GlobalJobManager.Companion.DISCONNECT_KEY
import com.thindie.rknzbl.application.work.GlobalJobManager.Companion.FETCH_KEY_HOME
import com.thindie.rknzbl.feature.home.domain.ConnectionProfileRepository
import kotlinx.coroutines.flow.combine

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
        val isConnected = s.connectedProfile != null || s.screenVpnState == ScreenVpnState.Running
        if (isConnected) {
          globalJobManager.launchGlobal(DISCONNECT_KEY) { repository.disconnect() }
        } else if (s.lastBestProfile != null) {
          // Fast path: a measured best profile is already known, reconnect without re-measuring.
          val profile = s.lastBestProfile
          globalJobManager.launchGlobal(CONNECT_KEY) { repository.connect(profile) }
        } else {
          // Slow path: measure first and auto-connect once the batch yields a best profile.
          repository.requestConnect()
          globalJobManager.launchGlobal(FETCH_KEY_HOME) { repository.fetch(false) }
        }
        null
      }
    }
  },
  section = HomeSection.Home,
  stateSink = { screenScope ->
    // The block must mutate state for the action to fire: transition() skips actions when the
    // resulting state is unchanged (e.g. a replayed best profile after route recreation).
    screenScope.sub(repository.lastMeasured).transition(
      block = { state, profile -> if (profile != null) state.copy(lastBestProfile = profile) else state },
      action = { _, _, profile ->
        // Auto-connect only with a pending connect intent; otherwise this is a stale replay
        // after an explicit disconnect.
        if (profile == null || !repository.takeConnectIntent()) return@transition
        globalJobManager.launchGlobal(CONNECT_KEY) { repository.connect(profile) }
      },
    )

    screenScope.sub(repository.connected).transition { state, connected ->
      state.copy(connectedProfile = connected)
    }

    screenScope.sub(globalJobManager.isRunning(FETCH_KEY_HOME)).transition { state, running ->
      state.copy(profilesLoading = running)
    }

    val connectRunning = globalJobManager.isRunning(CONNECT_KEY)
    screenScope.sub(connectRunning).transition { state, running ->
      if (running) state.copy(screenVpnState = ScreenVpnState.TurningOn) else state
    }

    val disconnectRunning = globalJobManager.isRunning(DISCONNECT_KEY)
    screenScope.sub(disconnectRunning).transition { state, running ->
      if (running) state.copy(screenVpnState = ScreenVpnState.TurningOff) else state
    }

    screenScope.sub(repository.vpnState).transition { state, vpn ->
      state.copy(
        screenVpnState =
          when (vpn) {
            is WorkState.Running -> ScreenVpnState.Running
            is WorkState.Idle -> ScreenVpnState.NotStarted
            is WorkState.Error -> ScreenVpnState.Error
          },
        vpnError = (vpn as? WorkState.Error)?.message,
      )
    }

    screenScope.sub(
      repository.stored.combine(repository.received) { stored, received ->
        (stored?.isNotEmpty() == true) || (received?.isNotEmpty() == true)
      },
    ).transition { state, hasProfiles ->
      state.copy(hasProfiles = hasProfiles)
    }
  },
  routeContent = ::HomeScreenContent,
)
