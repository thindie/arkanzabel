package com.thindie.rknzbl.feature.home.ui.newprofiles

import com.thindie.engine.core.ScreenScope
import com.thindie.engine.core.ServiceCommand
import com.thindie.engine.core.WorkState
import com.thindie.engine.core.stateSink
import com.thindie.engine.core.sub
import com.thindie.engine.core.transition
import com.thindie.rknzbl.application.Application
import com.thindie.rknzbl.feature.home.HomeFlow
import com.thindie.rknzbl.feature.managegate.gatelist.SelectSourceFlow
import com.thindie.rknzbl.feature.managegate.gatelist.resolveLabels
import com.v2ray.ang.runtime.SettingsManager
import com.v2ray.ang.runtime.SpeedtestManager
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest

fun HomeFlow.stateSink(screenScope: ScreenScope<ScreenState, ScreenCommand>) {
  stateSink(screenScope) { s ->
    s.sub(sourceChanges)
      .transition(
        action = { _, _, source ->
          when (source) {
            is SelectSourceFlow.Result.CustomSource,
            SelectSourceFlow.Result.FullBlackShadowSocks,
            SelectSourceFlow.Result.FullBlackVless,
            SelectSourceFlow.Result.MobileBlackVless,
            SelectSourceFlow.Result.NotSelected,
            SelectSourceFlow.Result.WhiteListAll,
            SelectSourceFlow.Result.WhiteListMobile,
            SelectSourceFlow.Result.WhiteListMobileV2,
            SelectSourceFlow.Result.WhiteListRussian,
            -> s.send(ScreenCommand.Refresh)

            SelectSourceFlow.Result.StoredProfiles -> {
              startStoredProfilesFlow { go(newProfiles()) }
            }
          }
        },
      ) { state, source ->
        state.copy(
          sourceName = source.resolveLabels(appContext).second,
          sourceUrl = source.sourceUrl.orEmpty(),
        )
      }

    s.sub(
      (appContext as Application)
        .applicationScope
        .settingsRepository
        .isCustomSourceEnabled
        .filter { it }
        .flatMapLatest {
          appContext
            .applicationScope
            .settingsRepository
            .customSourceUrl
            .filterNotNull()
            .map { SelectSourceFlow.Result.CustomSource(it) }
        },
    ).transition { state, source ->
      state.copy(
        sourceName = source.resolveLabels(appContext).second,
        sourceUrl = source.sourceUrl.orEmpty(),
      )
    }

    s.sub(
      (
        (appContext as Application)
          .profilePingManager
          .measureResults
      ),
    ).transition { state, results ->
      state.copy(
        pingResults = results,
        pingState = WorkState.Idle,
      )
    }

    s.sub(
      (
        (appContext as Application)
          .profilePingManager
          .lastMeasured
          .filterNotNull()
      ),
    ).transition { state, (profile, ping) ->
      val available = state.pingResults ?: emptyMap()
      state.copy(
        pingResults = available + (profile to ping),
      )
    }

    s.sub(
      selected
        .mapLatest { profile ->
          val result =
            when ((appContext as Application).vpnRuntimeState.value) {
              is WorkState.Error -> SpeedtestManager.SpeedTestResult.Err("Впн сервис упал")
              WorkState.Idle -> SpeedtestManager.SpeedTestResult.Err("Впн сервис не стартовал")
              WorkState.Running ->
                SpeedtestManager.testConnection(
                  context = appContext,
                  port = SettingsManager.getHttpPort(),
                )
            }
          profile to result
        },
    )
      .transition(
        action = { _, _, (_, result) ->
          when (result) {
            is SpeedtestManager.SpeedTestResult.Err ->
              s.sendEvent(ServiceCommand.UiEvent.SnackText(result.message))
            is SpeedtestManager.SpeedTestResult.Ok ->
              s.sendEvent(ServiceCommand.UiEvent.SnackText(result.message))
            null -> Unit
          }
        },
        block = { state, (profile, result) ->
          state.copy(
            selected = profile,
            selectedTestConnectionMessage = result,
          )
        },
      )
  }
}
