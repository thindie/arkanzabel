package com.thindie.rknzbl.appfeatures.settings.data

import android.content.Context
import com.thindie.rknzbl.appfeatures.settings.domain.AppRow
import com.thindie.rknzbl.appfeatures.settings.domain.PerAppProxyRepository
import com.v2ray.ang.AppConfig
import com.v2ray.ang.runtime.KeyValueStorage
import com.v2ray.ang.util.AppManagerUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class PerAppProxyRepositoryImpl(
  private val appContext: Context,
  private val storage: KeyValueStorage,
) : PerAppProxyRepository {
  private val selectedOnlySetting =
    Setting<Boolean>(
      read = { storage.decodeSettingsBool(AppConfig.PREF_PER_APP_PROXY) },
      write = { enabled ->
        storage.encodeSettings(AppConfig.PREF_PER_APP_PROXY, enabled)
        if (enabled) {
          storage.encodeSettings(AppConfig.PREF_BYPASS_APPS, false)
        }
      },
    )
  override val selectedOnly: Flow<Boolean> = selectedOnlySetting.flow

  override suspend fun setMode(selectedOnly: Boolean) {
    selectedOnlySetting.set(selectedOnly)
  }

  private val packagesSetting =
    Setting<Set<String>>(
      read = { storage.decodeSettingsStringSet(AppConfig.PREF_PER_APP_PROXY_SET) },
      write = { storage.encodeSettings(AppConfig.PREF_PER_APP_PROXY_SET, it) },
    )
  override val packages: Flow<Set<String>> = packagesSetting.flow

  override suspend fun setPackages(packages: Set<String>) {
    packagesSetting.set(packages)
  }

  override suspend fun loadInstalledApps(): List<AppRow> =
    withContext(Dispatchers.IO) {
      AppManagerUtil.loadAppsForPerAppTunneling(appContext)
        .map { AppRow(it.appName, it.packageName) }
        .sortedBy { it.appName.lowercase() }
    }
}
