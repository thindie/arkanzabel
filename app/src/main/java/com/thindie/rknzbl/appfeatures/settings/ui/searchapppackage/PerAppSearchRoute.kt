package com.thindie.rknzbl.appfeatures.settings.ui.searchapppackage

import com.thindie.engine.core.RouteFactory
import com.thindie.engine.core.ScreenScope
import com.thindie.engine.core.sub
import com.thindie.engine.core.transition
import com.thindie.rknzbl.appfeatures.settings.SettingsFlow
import com.thindie.rknzbl.appfeatures.settings.domain.PerAppProxyRepository

fun SettingsFlow.perAppSearch() =
  RouteFactory.create(
    initialState = SearchState(searchQuery = "", allApps = emptyList(), selectedPackages = emptySet()),
    execute = { c: PerAppSearchCommand, s: SearchState ->
      when (c) {
        PerAppSearchCommand.Back -> {
          back()
          null
        }

        PerAppSearchCommand.LoadApps ->
          s.copy(allApps = flowModule.perAppProxyRepository.loadInstalledApps())

        is PerAppSearchCommand.SetSearch -> s.copy(searchQuery = c.query)

        is PerAppSearchCommand.AddPackage -> {
          val next = s.selectedPackages + c.packageName
          flowModule.perAppProxyRepository.setPackages(next)
          s.copy(selectedPackages = next, searchQuery = "")
        }
      }
    },
    stateSink = { screenScope -> perAppSearchStateSink(screenScope, flowModule.perAppProxyRepository) },
    id = "SettingsFlow-perapp-search",
    initialCommand =
      RouteFactory.InitialCommand {
        PerAppSearchCommand.LoadApps as PerAppSearchCommand
      },
    routeContent = ::PerAppSearchScreen,
  )

internal fun perAppSearchStateSink(
  screenScope: ScreenScope<SearchState, PerAppSearchCommand>,
  repository: PerAppProxyRepository,
) {
  screenScope.sub(repository.packages)
    .transition { state, packages ->
      state.copy(selectedPackages = packages)
    }
}
