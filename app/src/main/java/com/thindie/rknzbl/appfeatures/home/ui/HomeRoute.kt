package com.thindie.rknzbl.appfeatures.home.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.thindie.engine.core.RouteFactory
import com.thindie.engine.core.ServiceCommand
import com.thindie.engine.core.WorkState
import com.thindie.engine.core.sub
import com.thindie.engine.core.transition
import com.thindie.engine.uikit.Action
import com.thindie.rknzbl.R
import com.thindie.rknzbl.appfeatures.home.HomeSection
import com.thindie.rknzbl.application.work.GlobalJobManager
import com.thindie.rknzbl.application.work.GlobalJobManager.Companion.CONNECT_KEY
import com.thindie.rknzbl.application.work.GlobalJobManager.Companion.DISCONNECT_KEY
import com.thindie.rknzbl.application.work.GlobalJobManager.Companion.FETCH_KEY_HOME
import com.thindie.rknzbl.appversion.AppVersionResolver
import com.thindie.rknzbl.domain.ConnectionProfileRepository
import kotlinx.coroutines.flow.combine

/** Where a tap on the update prompt lands: the project's release page in the browser. */
private const val RELEASE_URL = "https://github.com/thindie/arkanzabel"

@Suppress("FunctionName", "MagicNumber")
internal fun HomeRoute(
  repository: ConnectionProfileRepository,
  globalJobManager: GlobalJobManager,
  context: Context,
  appVersionResolver: AppVersionResolver,
) = RouteFactory.create(
  id = "HomeFlow-home",
  initialState = ScreenState(),
  execute = { c: ScreenCommand, s: ScreenState ->
    when (c) {
      ScreenCommand.ToggleConnect -> {
        when (s.screenVpnState) {
          // Error counts as disconnected: a tap retries the connect.
          ScreenVpnState.NotStarted, ScreenVpnState.Error -> {
            val best = s.lastBestProfile
            if (best != null) {
              // Fast path: a measured best profile is already known, reconnect without re-measuring.
              globalJobManager.launchGlobal(CONNECT_KEY) { repository.connect(best) }
            } else if (s.hasProfiles) {
              // Slow path: three-stage measurement, connect to the best profile.
              globalJobManager.launchGlobal(CONNECT_KEY) {
                val measured = repository.measureStaged() ?: return@launchGlobal
                repository.connect(measured)
              }
            }
          }
          ScreenVpnState.TurningOff, ScreenVpnState.TurningOn -> null // busy: ignore taps
          ScreenVpnState.Running -> globalJobManager.launchGlobal(DISCONNECT_KEY) { repository.disconnect() }
        }
        null
      }

      ScreenCommand.RefreshProfiles -> {
        globalJobManager.launchGlobal(FETCH_KEY_HOME) { repository.fetch(true) }
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

    // Offer an update once a strictly newer remote version is known. The engine fires the action
    // only when state changes, so the updateShown flag makes the snack appear exactly once; its
    // tap opens the release page in the browser and it auto-dismisses on its own.
    screenScope.sub(appVersionResolver.remoteVersion).transition(
      block = { state, remote ->
        if (!state.updateShown && appVersionResolver.isUpdateAvailable(remote)) {
          state.copy(updateShown = true)
        } else {
          state
        }
      },
      action = { _, _, _ ->
        screenScope.sendEvent(
          ServiceCommand.UiEvent.Snack(
            Action(
              listener = { openReleaseInBrowser(context) },
              resRef = R.string.app_version_update_snack,
            ),
          ),
        )
      },
    )
  },
  routeContent = ::HomeScreenContent,
)

/** Opens [RELEASE_URL] in the default browser. Application context is enough with NEW_TASK. */
private fun openReleaseInBrowser(context: Context) {
  context.startActivity(
    Intent(Intent.ACTION_VIEW, Uri.parse(RELEASE_URL))
      .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
  )
}
