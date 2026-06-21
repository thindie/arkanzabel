package com.thindie.rknzbl.feature.managegate.storedgates.profiles

import com.thindie.rknzbl.R
import com.thindie.rknzbl.application.Application
import com.thindie.rknzbl.engine.ScreenScope
import com.thindie.rknzbl.engine.ServiceCommand
import com.thindie.rknzbl.engine.WorkState
import com.thindie.rknzbl.engine.stateSink
import com.thindie.rknzbl.engine.sub
import com.thindie.rknzbl.engine.transition
import com.thindie.rknzbl.feature.managegate.storedgates.FavoriteProfilesFlow
import com.thindie.rknzbl.uikit.Action
import com.v2ray.ang.runtime.KeyValueStorage
import com.v2ray.ang.runtime.SettingsManager
import com.v2ray.ang.runtime.SpeedtestManager
import com.v2ray.ang.runtime.V2RayServiceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import java.util.UUID

internal fun FavoriteProfilesFlow.stateSink(screenScope: ScreenScope<ScreenState, ScreenCommand>) {
  screenScope.stateSink {
    sub(
      selected
        .mapLatest {
          val port = it.serverPort?.toIntOrNull() ?: SettingsManager.getHttpPort()
          it to
            SpeedtestManager.testConnection(
              context = appContext,
              port = port,
            )
        },
    )
      .transition(
        action = { _, _, (_, result) ->
          when (result) {
            is SpeedtestManager.SpeedTestResult.Err ->
              sendEvent(
                ServiceCommand.UiEvent.SnackText(result.message),
              )
            is SpeedtestManager.SpeedTestResult.Ok -> {
              sendEvent(ServiceCommand.UiEvent.SnackText(result.message))
            }
            null -> Unit
          }
        },
        block = { s, (profile, result) ->
          s.copy(
            selected = profile,
            selectedTestConnectionMessage = result,
          )
        },
      )

    sub(startVpn)
      .transition(
        action = { _, _, _ ->
          sendEvent(
            ServiceCommand.UiEvent.Snack(
              action =
                Action(
                  resRef = R.string.home_starting_vpn,
                  listener = { },
                ),
            ),
          )
        },
      )
  }
}

internal suspend fun FavoriteProfilesFlow.exec(
  c: ScreenCommand,
  s: ScreenState,
): ScreenState {
  return when (c) {
    ScreenCommand.BackRequested -> {
      finish(Unit)
      s
    }

    ScreenCommand.Dismissed -> {
      finish(Unit)
      s
    }

    ScreenCommand.RequestStoredProfiles -> {
      withContext(Dispatchers.IO) {
        val profiles = repository.read()
        val active = repository.activeProfile()
        if (active != null) {
          selected.tryEmit(active)
        }
        s.copy(
          profiles = profiles,
          selected = active,
        )
      }
    }

    is ScreenCommand.Delete -> {
      withContext(Dispatchers.IO) {
        if (c.profile == s.selected) {
          repository.delete(c.profile)
          V2RayServiceManager.stopVService(appContext)
          val profiles = repository.read()
          s.copy(
            profiles = profiles,
            selectedProfiles = emptySet(),
            selectionMode = false,
          )
        } else {
          val selected = s.selectedProfiles
          repository.delete(c.profile)
          val profiles = repository.read()
          s.copy(
            selectedProfiles = selected,
            profiles = profiles,
          )
        }
      }
    }

    is ScreenCommand.Activate -> {
      withContext(Dispatchers.Default) {
        V2RayServiceManager.startVService(
          context = appContext,
          guid =
            KeyValueStorage.encodeServerConfig(
              guid = UUID.randomUUID().toString(),
              config = c.profile,
            ),
        )
        startVpn.tryEmit(Unit)
        (appContext as Application).vpnRuntimeState.filterNot { it is WorkState.Idle }.first()
        selected.tryEmit(c.profile)
        s.copy(
          selected = c.profile,
        )
      }
    }

    ScreenCommand.StopService -> {
      V2RayServiceManager.stopVService(appContext)
      s
    }

    is ScreenCommand.EnterMultiDeletionMode -> {
      s.copy(selectionMode = true)
    }

    is ScreenCommand.TogglePendingDelete -> {
      if (c.profile in s.selectedProfiles) {
        s.copy(
          selectedProfiles = s.selectedProfiles - c.profile,
        )
      } else {
        s.copy(
          selectedProfiles = s.selectedProfiles + c.profile,
        )
      }
    }

    is ScreenCommand.ExitMultiDeletionMode -> {
      s.copy(selectedProfiles = emptySet(), selectionMode = false)
    }

    ScreenCommand.BatchDelete -> {
      withContext(Dispatchers.IO) {
        var hasActiveProfile = false
        for (profile in s.selectedProfiles) {
          repository.delete(profile)
          if (s.selected?.subscriptionId == profile.subscriptionId) {
            hasActiveProfile = true
          }
        }

        if (hasActiveProfile) {
          V2RayServiceManager.stopVService(appContext)
        }
        val profiles = repository.read()
        s.copy(
          profiles = profiles,
          selectedProfiles = emptySet(),
          selectionMode = false,
          selected = if (hasActiveProfile) null else s.selected,
        )
      }
    }
  }
}
