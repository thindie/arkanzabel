package com.thindie.rknzbl.feature.home.ui.newprofiles

import com.thindie.rknzbl.R
import com.thindie.rknzbl.engine.ScreenScope
import com.thindie.rknzbl.engine.ServiceCommand
import com.thindie.rknzbl.engine.stateSink
import com.thindie.rknzbl.engine.sub
import com.thindie.rknzbl.engine.transition
import com.thindie.rknzbl.feature.home.HomeFlow
import com.thindie.rknzbl.feature.managegate.gatelist.SelectSourceFlow
import com.thindie.rknzbl.feature.managegate.gatelist.resolveLabels
import com.thindie.rknzbl.uikit.Action
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
            is SpeedtestManager.SpeedTestResult.Err -> {
              println("SPEEDTEST ERR")
              sendEvent(
                ServiceCommand.UiEvent.SnackText(result.message),
              )
            }
            is SpeedtestManager.SpeedTestResult.Ok -> {
              println("SPEEDTEST OK")
              sendEvent(ServiceCommand.UiEvent.SnackText(result.message))
            }
            null -> {
              println("SPEEDTEST NULL")
            }
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
