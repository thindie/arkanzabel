package com.thindie.rknzbl.feature.home.ui.newprofiles

import com.thindie.rknzbl.engine.RouteFactory
import com.thindie.rknzbl.feature.home.HomeFlow
import com.thindie.rknzbl.feature.managegate.gatelist.SelectSourceFlow
import com.thindie.rknzbl.feature.managegate.gatelist.resolveLabels
import com.v2ray.ang.runtime.KeyValueStorage
import com.v2ray.ang.runtime.ProfileUriParser
import com.v2ray.ang.runtime.V2RayServiceManager
import com.v2ray.ang.util.HttpUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
    routeContent = { NewProfiles() },
    id = "Home-main",
    initialCommand =
      RouteFactory.InitialCommand {
        ScreenCommand.Start as ScreenCommand
      },
  )

private suspend fun HomeFlow.exec(
  command: ScreenCommand,
  homeState: ScreenState,
): ScreenState {
  return when (command) {
    is ScreenCommand.Back -> {
      back()
      homeState
    }

    is ScreenCommand.Select -> {
      withContext(Dispatchers.Default) {
        V2RayServiceManager.startVService(
          context = appContext,
          guid =
            KeyValueStorage.encodeServerConfig(
              guid = UUID.randomUUID().toString(),
              config = command.profile,
            ),
        )
        homeState.copy(selected = command.profile, serviceBeingStarted = true)
      }
    }

    ScreenCommand.Start -> {
      withContext(Dispatchers.IO) {
        val links =
          HttpUtil.getUrlContent(
            url = homeState.sourceUrl,
            timeout = 10_000,
          )
        val parsed =
          links?.split("\n")
            ?.map { uri ->
              async { ProfileUriParser.parse(uri) }
            }
            ?.awaitAll()
            ?.mapNotNull { it }
        homeState.copy(links = parsed.orEmpty())
      }
    }

    ScreenCommand.Stop -> {
      V2RayServiceManager.stopVService(appContext)
      homeState
    }

    ScreenCommand.Refresh -> {
      withContext(Dispatchers.IO) {
        val links =
          HttpUtil.getUrlContent(
            url = homeState.sourceUrl,
            timeout = 10_000,
          )
        val parsed =
          links?.split("\n")
            ?.map { uri ->
              async { ProfileUriParser.parse(uri) }
            }
            ?.awaitAll()
            ?.mapNotNull { it }
        homeState.copy(links = parsed ?: homeState.links)
      }
    }

    ScreenCommand.Dismissed -> {
      homeState.copy(selected = null, established = false)
    }

    ScreenCommand.Choose -> {
      startSelectSourceFlow()
      homeState
    }

    is ScreenCommand.Save -> {
      val guid = KeyValueStorage.getSelectServer()
      repository.save(requireNotNull(guid))
      homeState
    }

    ScreenCommand.OpenPerAppProxy -> {
      startPerAppProxyFlow()
      homeState
    }
  }
}
