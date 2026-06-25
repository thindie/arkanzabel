package com.thindie.rknzbl.feature.perapp

import com.thindie.rknzbl.engine.RouteFactory
import com.v2ray.ang.AppConfig
import com.v2ray.ang.runtime.KeyValueStorage
import com.v2ray.ang.util.AppManagerUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun PerAppProxyFlow.main() =
  RouteFactory.create(
    initialState =
      State(
        mode =
          if (KeyValueStorage.decodeSettingsBool(AppConfig.PREF_PER_APP_PROXY)) {
            ProxyScopeMode.Selected
          } else {
            ProxyScopeMode.All
          },
        allApps = emptyList(),
        selectedPackages =
          KeyValueStorage.decodeSettingsStringSet(AppConfig.PREF_PER_APP_PROXY_SET)
            ?: emptySet(),
      ),
    execute = ::execMain,
    routeContent = { PerAppProxyScreen() },
    id = "PerApp-main",
    initialCommand =
      RouteFactory.InitialCommand {
        PerAppProxyCommand.LoadApps as PerAppProxyCommand
      },
  )

internal suspend fun PerAppProxyFlow.execMain(
  command: PerAppProxyCommand,
  state: State,
): State {
  return when (command) {
    PerAppProxyCommand.Back -> {
      finish(Unit)
      state
    }

    PerAppProxyCommand.LoadApps -> {
      val rows =
        withContext(Dispatchers.IO) {
          AppManagerUtil.loadAppsForPerAppTunneling(appContext)
            .map { AppRow(it.appName, it.packageName) }
            .sortedBy { it.appName.lowercase() }
        }
      state.copy(allApps = rows)
    }

    PerAppProxyCommand.RefreshFromStorage ->
      state.copy(
        mode =
          if (KeyValueStorage.decodeSettingsBool(AppConfig.PREF_PER_APP_PROXY)) {
            ProxyScopeMode.Selected
          } else {
            ProxyScopeMode.All
          },
        selectedPackages =
          KeyValueStorage.decodeSettingsStringSet(AppConfig.PREF_PER_APP_PROXY_SET)
            ?: emptySet(),
      )

    PerAppProxyCommand.SetModeAll -> {
      persistModeAll()
      state.copy(mode = ProxyScopeMode.All)
    }

    PerAppProxyCommand.SetModeSelected -> {
      persistModeSelected()
      state.copy(mode = ProxyScopeMode.Selected)
    }

    PerAppProxyCommand.OpenSearch -> {
      go(search())
      state
    }

    is PerAppProxyCommand.RemovePackage -> {
      val next = state.selectedPackages - command.packageName
      persistPackages(next)
      state.copy(selectedPackages = next)
    }
  }
}

internal fun PerAppProxyFlow.persistModeAll() {
  KeyValueStorage.encodeSettings(AppConfig.PREF_PER_APP_PROXY, false)
}

internal fun PerAppProxyFlow.persistModeSelected() {
  KeyValueStorage.encodeSettings(AppConfig.PREF_PER_APP_PROXY, true)
  KeyValueStorage.encodeSettings(AppConfig.PREF_BYPASS_APPS, false)
}

internal fun PerAppProxyFlow.persistPackages(set: Set<String>) {
  KeyValueStorage.encodeSettings(AppConfig.PREF_PER_APP_PROXY_SET, set)
}
