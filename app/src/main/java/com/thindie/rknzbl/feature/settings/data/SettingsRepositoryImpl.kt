package com.thindie.rknzbl.feature.settings.data

import com.thindie.rknzbl.feature.settings.domain.SettingsRepository
import com.thindie.rknzbl.feature.settings.data.theme.toChoice
import com.thindie.rknzbl.feature.settings.data.theme.toStorageString
import com.thindie.rknzbl.uikit.ThemeSwitcher
import com.v2ray.ang.runtime.KeyValueStorage

/**
 * Implementation of [SettingsRepository] using [KeyValueStorage].
 */
class SettingsRepositoryImpl(
  private val storage: KeyValueStorage,
) : SettingsRepository {
  override suspend fun getThemeMode(): ThemeSwitcher.Choice? =
    storage.getThemeMode()?.toChoice()

  override suspend fun setThemeMode(mode: ThemeSwitcher.Choice): Boolean =
    storage.setThemeMode(mode.toStorageString())

  override suspend fun isAutosaveEnabled(): Boolean =
    storage.isAutosaveEnabled()

  override suspend fun toggleAutosave(enabled: Boolean): Boolean =
    storage.setAutosaveMode(enabled)
}
