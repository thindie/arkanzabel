package com.thindie.rknzbl.appfeatures.settings.ui.perapp

import com.thindie.engine.core.RouteFactory
import com.thindie.engine.core.ScreenScope
import com.thindie.engine.core.sub
import com.thindie.engine.core.transition
import com.thindie.rknzbl.appfeatures.settings.SettingsFlow
import com.thindie.rknzbl.appfeatures.settings.domain.PerAppProxyRepository
import com.thindie.rknzbl.appfeatures.settings.ui.searchapppackage.perAppSearch

fun SettingsFlow.perAppMain() =
  RouteFactory.create(
    initialState =
      PerAppViewState(
        mode = ProxyScopeMode.All,
        allApps = emptyList(),
        selectedPackages = emptySet(),
      ),
    execute = { c: PerAppCommand, s: PerAppViewState ->
      when (c) {
        PerAppCommand.Back -> {
          back()
          null
        }

        PerAppCommand.SetModeAll -> {
          flowModule.perAppProxyRepository.setMode(selectedOnly = false)
          null
        }

        PerAppCommand.SetModeSelected -> {
          flowModule.perAppProxyRepository.setMode(selectedOnly = true)
          null
        }

        PerAppCommand.OpenSearch -> {
          go(perAppSearch())
          null
        }

        is PerAppCommand.RemovePackage -> {
          val next = s.selectedPackages - c.packageName
          flowModule.perAppProxyRepository.setPackages(next)
          null
        }
      }
    },
    stateSink = { screenScope -> perAppMainStateSink(screenScope, flowModule.perAppProxyRepository) },
    id = "SettingsFlow-perapp-main",
    routeContent = ::PerAppProxyScreen,
  )

private fun perAppMainStateSink(
  screenScope: ScreenScope<PerAppViewState, PerAppCommand>,
  repository: PerAppProxyRepository,
) {
  screenScope.sub(repository.selectedOnly)
    .transition { state, selected ->
      state.copy(mode = if (selected) ProxyScopeMode.Selected else ProxyScopeMode.All)
    }

  screenScope.sub(repository.packages)
    .transition { state, packages ->
      state.copy(selectedPackages = packages)
    }
}
