package com.thindie.rknzbl.appfeatures.settings.ui

import android.os.Build
import com.thindie.engine.core.RouteFactory
import com.thindie.rknzbl.appfeatures.home.HomeSection
import com.thindie.rknzbl.appfeatures.settings.SettingsFlow
import com.thindie.rknzbl.appfeatures.settings.domain.SettingsRepository
import com.thindie.rknzbl.feature.home.domain.ConnectionProfileRepository

fun SettingsFlow.settings(
  repository: SettingsRepository,
  connectionProfileRepository: ConnectionProfileRepository,
) = RouteFactory.create(
  initialState = ScreenState(),
  execute = { c: ScreenCommand, s: ScreenState ->
    when (c) {
      is ScreenCommand.ToggleAutosave -> {
        val current = s.autosaveEnabled ?: true
        repository.toggleAutosave(!current)
        null
      }

      ScreenCommand.Back -> {
        back()
        null
      }

      is ScreenCommand.SelectLanguage -> {
        repository.setLanguage(c.languageCode)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          flowModule.updateLocale(c.languageCode)
          null
        } else {
          s.copy(legacyRestart = true)
        }
      }

      ScreenCommand.ToggleMux -> {
        val current = s.muxEnabled ?: false
        repository.toggleMux(!current)
        null
      }

      ScreenCommand.ToggleStorageMode -> {
        val current = s.isLocalSave ?: false
        repository.toggleLocalSave(!current)
        connectionProfileRepository.invalidateCaches()
        null
      }

      ScreenCommand.ToggleSpeed -> {
        val current = s.speedEnabled ?: false
        repository.toggleSpeed(!current)
        null
      }

      ScreenCommand.ToggleNewDesign -> {
        val current = s.useNewDesign ?: false
        repository.toggleUseNewDesign(!current)
        null
      }
    }
  },
  stateSink = { screenScope -> settingsStateSink(screenScope, repository) },
  id = "SettingsFlow-settings",
  section = HomeSection.Settings,
  routeContent = ::SettingsScreenContent,
)
