package com.thindie.rknzbl.feature.perapp

import com.thindie.rknzbl.engine.RouteFactory
import com.v2ray.ang.AppConfig
import com.v2ray.ang.runtime.KeyValueStorage
import com.v2ray.ang.util.AppManagerUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun PerAppProxyFlow.search() =
  RouteFactory.create(
    initialState =
      SearchState(
        searchQuery = "",
        allApps = emptyList(),
        selectedPackages =
          KeyValueStorage.decodeSettingsStringSet(AppConfig.PREF_PER_APP_PROXY_SET)
            ?: emptySet(),
      ),
    execute = ::execSearch,
    routeContent = { PerAppSearchScreen() },
    id = "Perapp-id",
    initialCommand =
      RouteFactory.InitialCommand {
        PerAppSearchCommand.LoadApps as PerAppSearchCommand
      },
  )

internal suspend fun PerAppProxyFlow.execSearch(
  command: PerAppSearchCommand,
  state: SearchState,
): SearchState {
  return when (command) {
    PerAppSearchCommand.Back -> {
      back()
      state
    }

    PerAppSearchCommand.LoadApps -> {
      val rows =
        withContext(Dispatchers.IO) {
          AppManagerUtil.loadAppsForPerAppTunneling(appContext)
            .map { AppRow(it.appName, it.packageName) }
            .sortedBy { it.appName.lowercase() }
        }
      val sel =
        KeyValueStorage.decodeSettingsStringSet(AppConfig.PREF_PER_APP_PROXY_SET) ?: emptySet()
      state.copy(allApps = rows, selectedPackages = sel)
    }

    is PerAppSearchCommand.SetSearch ->
      state.copy(searchQuery = command.query)

    is PerAppSearchCommand.AddPackage -> {
      val next = state.selectedPackages + command.packageName
      persistPackages(next)
      state.copy(selectedPackages = next, searchQuery = "")
    }
  }
}
