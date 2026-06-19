package com.thindie.rknzbl.feature.home.ui.newprofiles

import com.thindie.rknzbl.application.Application
import com.thindie.rknzbl.engine.ScreenScope
import com.thindie.rknzbl.engine.ServiceCommand
import com.thindie.rknzbl.engine.WorkState
import com.thindie.rknzbl.engine.stateSink
import com.thindie.rknzbl.engine.sub
import com.thindie.rknzbl.engine.transition
import com.thindie.rknzbl.feature.home.HomeFlow
import com.thindie.rknzbl.feature.managegate.gatelist.SelectSourceFlow
import com.thindie.rknzbl.feature.managegate.gatelist.resolveLabels
import com.v2ray.ang.runtime.SettingsManager
import com.v2ray.ang.runtime.SpeedtestManager
import kotlinx.coroutines.flow.map

fun HomeFlow.stateSink(screenScope: ScreenScope<ScreenState, ScreenCommand>) {
  screenScope.stateSink {
    sub(sourceChanges)
      .transition(
        action = { _, _, source ->
          when (source) {
            SelectSourceFlow.Result.FullBlackShadowSocks,
            SelectSourceFlow.Result.FullBlackVless,
            SelectSourceFlow.Result.MobileBlackVless,
            SelectSourceFlow.Result.NotSelected,
            SelectSourceFlow.Result.WhiteListAll,
            SelectSourceFlow.Result.WhiteListMobile,
            SelectSourceFlow.Result.WhiteListMobileV2,
            SelectSourceFlow.Result.WhiteListRussian,
            -> send(ScreenCommand.Refresh)

            SelectSourceFlow.Result.StoredProfiles -> {
              startStoredProfilesFlow { go(newProfiles()) }
            }
          }
        },
      ) { s, source ->
        val newState =
          s.copy(
            sourceName = source.resolveLabels(appContext).second,
            sourceUrl = source.sourceUrl.orEmpty(),
          )
        newState
      }

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
        s
      }
  }
}
