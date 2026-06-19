package com.thindie.rknzbl.feature.managegate.storedgates.profiles

import com.thindie.rknzbl.application.Application
import com.thindie.rknzbl.engine.ScreenScope
import com.thindie.rknzbl.engine.ServiceCommand
import com.thindie.rknzbl.engine.WorkState
import com.thindie.rknzbl.engine.stateSink
import com.thindie.rknzbl.engine.sub
import com.thindie.rknzbl.engine.transition
import com.thindie.rknzbl.feature.managegate.storedgates.FavoriteProfilesFlow
import com.v2ray.ang.runtime.KeyValueStorage
import com.v2ray.ang.runtime.SettingsManager
import com.v2ray.ang.runtime.SpeedtestManager
import com.v2ray.ang.runtime.V2RayServiceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

internal fun FavoriteProfilesFlow.stateSink(screenScope: ScreenScope<ScreenState, ScreenCommand>) {
  screenScope.stateSink {
    // Flow 1: Immediate UI state updates — no speed test blocking
    sub(
      (appContext as Application).vpnRuntimeState.map { state ->
        when (state) {
          is WorkState.Error -> state to null
          WorkState.Idle -> state to null
          WorkState.Running -> state to null
        }
      },
    )
      .transition({ _, _, (vpnState, _) ->
        when (vpnState) {
          is WorkState.Error -> send(ScreenCommand.Dismissed)
          WorkState.Idle -> Unit
          WorkState.Running -> Unit
        }
      }) { s, (vpnState, _) ->
        val profiles = repository.read()
        val selected =
          when (vpnState) {
            WorkState.Idle -> null
            is WorkState.Error,
            WorkState.Running,
            -> {
              repository.activeProfile()
            }
          }
        s.copy(
          serviceBeingStarted =
            when (vpnState) {
              is WorkState.Error -> null
              WorkState.Idle -> true
              WorkState.Running -> null
            }.takeIf { s.serviceBeingStarted != null },
          established =
            when (vpnState) {
              is WorkState.Error -> false
              WorkState.Idle -> false
              WorkState.Running -> true
            },
          selectedTestConnectionMessage = null,
          showLoading = false,
          selected = selected,
          profiles = profiles,
        )
      }

    // Flow 2: Background speed test — only runs when service is running
    sub(
      (appContext as Application).vpnRuntimeState.map { state ->
        when (state) {
          is WorkState.Error -> null
          WorkState.Idle -> null
          WorkState.Running -> {
            val port = screenScope.state.value.selected?.serverPort
            if (port != null) {
              SpeedtestManager.testConnection(
                appContext,
                SettingsManager.getHttpPort(),
              )
            } else {
              null
            }
          }
        }
      },
    )
      .transition(
        action = { _, _, speedTestMessage ->
          if (speedTestMessage != null) {
            sendEvent(ServiceCommand.UiEvent.SnackText(speedTestMessage.message))
          }
        },
      ) { s, speedTestMessage ->
        s.copy(selectedTestConnectionMessage = speedTestMessage)
      }
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
        s.copy(
          profiles = profiles,
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
        s.copy(
          selected = c.profile,
          serviceBeingStarted = true,
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
