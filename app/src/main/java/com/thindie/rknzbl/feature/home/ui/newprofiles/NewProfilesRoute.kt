package com.thindie.rknzbl.feature.home.ui.newprofiles

import com.thindie.engine.core.RouteFactory
import com.thindie.engine.core.ScreenScopeError
import com.thindie.engine.core.WorkState
import com.thindie.rknzbl.R
import com.thindie.rknzbl.application.Application
import com.thindie.rknzbl.error.AppError
import com.thindie.rknzbl.feature.home.HomeFlow
import com.thindie.rknzbl.feature.managegate.gatelist.SelectSourceFlow
import com.thindie.rknzbl.feature.managegate.gatelist.resolveLabels
import com.v2ray.ang.runtime.KeyValueStorage
import com.v2ray.ang.runtime.V2RayServiceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.UUID

fun HomeFlow.newProfiles() =
  RouteFactory.create(
    initialState =
      ScreenState(
        sourceName = SelectSourceFlow.Result.MobileBlackVless.resolveLabels(appContext).first,
        sourceUrl = SelectSourceFlow.Result.MobileBlackVless.sourceUrl,
      ),
    execute = ::exec,
    stateSink = ::stateSink,
    routeContent = ::NewProfiles,
    id = "Home-main",
    initialCommand =
      RouteFactory.InitialCommand {
        ScreenCommand.Start as ScreenCommand
      },
    errorMapper = ::newProfilesErrorMapper,
  )

private fun HomeFlow.newProfilesErrorMapper(e: Throwable): ScreenScopeError {
  return when (e) {
    is AppError.ServerError.TimeOut ->
      ScreenScopeError(
        message = appContext.getString(R.string.error_timeout),
        actions =
          mapOf(
            ScreenScopeError.Actions.Common.ButtonSecondaryRetry to ScreenCommand.Refresh,
            ScreenScopeError.Actions.Common.ButtonMain to ScreenCommand.Back,
          ),
      )

    is AppError.ServerError.ConnectionFailed ->
      ScreenScopeError(
        message = appContext.getString(R.string.error_connection_failed),
        actions =
          mapOf(
            ScreenScopeError.Actions.Common.ButtonSecondaryRetry to ScreenCommand.Refresh,
            ScreenScopeError.Actions.Common.ButtonMain to ScreenCommand.Back,
          ),
      )

    is AppError.ServerError.HttpRequestFailed ->
      ScreenScopeError(
        message = appContext.getString(R.string.error_http_failed, e.statusCode),
        actions = mapOf(ScreenScopeError.Actions.Common.ButtonMain to ScreenCommand.Back),
      )

    else ->
      ScreenScopeError(
        message = appContext.getString(R.string.error_unexpected),
        actions =
          mapOf(
            ScreenScopeError.Actions.Common.ButtonSecondaryRetry to ScreenCommand.Refresh,
            ScreenScopeError.Actions.Common.ButtonMain to ScreenCommand.Back,
          ),
      )
  }
}

private suspend fun HomeFlow.exec(
  command: ScreenCommand,
  homeState: ScreenState,
): ScreenState? {
  return when (command) {
    is ScreenCommand.Back -> {
      back()
      null
    }

    is ScreenCommand.Select -> {
      withContext(Dispatchers.Default) {
        val guid =
          KeyValueStorage.encodeServerConfig(
            guid = UUID.randomUUID().toString(),
            config = command.profile,
          )
        V2RayServiceManager.startVService(
          context = appContext,
          guid = guid,
        )
        (appContext as Application).vpnRuntimeState.filter { it is WorkState.Idle }.first()
        appContext.vpnRuntimeState.filterNot { it is WorkState.Idle }.first()
        // Persist a fresh latency reading for the just-saved profile.
        (appContext as Application).applicationScope.pingManager.pingSaved(guid)
        selected.tryEmit(command.profile)
        homeState.copy(
          selected = command.profile,
          selectedTestConnectionMessage = null,
        )
      }
    }

    ScreenCommand.Start -> {
      withContext(Dispatchers.IO) {
        val profiles = repository.readFromSource(homeState.sourceUrl)
        (appContext as Application).applicationScope.pingManager.pingProfiles(
          profiles = profiles,
          force = settingsRepository.forceProfileMeasure.first(),
        )
        homeState.copy(
          links = profiles,
          pingState = WorkState.Running,
        )
      }
    }

    ScreenCommand.Stop -> {
      V2RayServiceManager.stopVService(appContext)
      null
    }

    ScreenCommand.Refresh -> {
      withContext(Dispatchers.IO) {
        repository.invalidateRemoteCache(homeState.sourceUrl)
        val profiles = repository.readFromSource(homeState.sourceUrl)
        (appContext as Application).applicationScope.pingManager.pingProfiles(
          profiles = profiles,
          force = settingsRepository.forceProfileMeasure.first(),
        )
        homeState.copy(
          links = profiles,
          pingState = WorkState.Running,
        )
      }
    }

    ScreenCommand.Dismissed -> {
      homeState.copy(selected = null)
    }

    ScreenCommand.Choose -> {
      startSelectSourceFlow()
      null
    }

    is ScreenCommand.Save -> {
      val guid = KeyValueStorage.getSelectServer()
      repository.save(requireNotNull(guid))
      null
    }

    ScreenCommand.OpenPerAppProxy -> {
      startPerAppProxyFlow()
      null
    }

    is ScreenCommand.Filter -> {
      homeState.copy(filter = command.mode)
    }
  }
}
