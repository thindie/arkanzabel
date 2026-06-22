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
import kotlinx.coroutines.flow.mapLatest

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

    sub(
      selected
        .mapLatest { profile ->
          val result =
            when ((appContext as Application).vpnRuntimeState.value) {
              is WorkState.Error -> {
                SpeedtestManager.SpeedTestResult.Err("Впн сервис упал")
              }
              WorkState.NotRunning -> {
                SpeedtestManager.SpeedTestResult.Err("Впн сервис не стартовал")
              }
              WorkState.Running -> {
                SpeedtestManager.testConnection(
                  context = appContext,
                  port = SettingsManager.getHttpPort(),
                )
              }
            }
          profile to result
        },
    )
      .transition(
        action = { _, _, (_, result) ->
          when (result) {
            is SpeedtestManager.SpeedTestResult.Err -> {
              sendEvent(
                ServiceCommand.UiEvent.SnackText(result.message),
              )
            }
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
  }
}
