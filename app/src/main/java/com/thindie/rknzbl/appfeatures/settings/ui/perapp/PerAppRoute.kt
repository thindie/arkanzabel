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
    execute = { c: PerAppProxyCommand, s: PerAppViewState ->
      when (c) {
        PerAppProxyCommand.Back -> {
          back()
          null
        }

        PerAppProxyCommand.LoadApps ->
          s.copy(allApps = flowModule.perAppProxyRepository.loadInstalledApps())

        PerAppProxyCommand.SetModeAll -> {
          flowModule.perAppProxyRepository.setMode(selectedOnly = false)
          null
        }

        PerAppProxyCommand.SetModeSelected -> {
          flowModule.perAppProxyRepository.setMode(selectedOnly = true)
          null
        }

        PerAppProxyCommand.OpenSearch -> {
          go(perAppSearch())
          null
        }

        is PerAppProxyCommand.RemovePackage -> {
          val next = s.selectedPackages - c.packageName
          flowModule.perAppProxyRepository.setPackages(next)
          null
        }
      }
    },
    stateSink = { screenScope -> perAppMainStateSink(screenScope, flowModule.perAppProxyRepository) },
    id = "SettingsFlow-perapp-main",
    initialCommand =
      RouteFactory.InitialCommand {
        PerAppProxyCommand.LoadApps as PerAppProxyCommand
      },
    routeContent = ::PerAppProxyScreen,
  )

private fun perAppMainStateSink(
  screenScope: ScreenScope<PerAppViewState, PerAppProxyCommand>,
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
