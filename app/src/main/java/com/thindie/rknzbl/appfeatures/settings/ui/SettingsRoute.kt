package com.thindie.rknzbl.appfeatures.settings.ui

import android.os.Build
import com.thindie.engine.core.RouteFactory
import com.thindie.rknzbl.appfeatures.home.HomeSection
import com.thindie.rknzbl.appfeatures.settings.SettingsFlow
import com.thindie.rknzbl.appfeatures.settings.ui.perapp.perAppMain
import com.thindie.rknzbl.appfeatures.settings.ui.source.source
import com.thindie.rknzbl.appfeatures.settings.ui.webdav.webdav
import com.thindie.rknzbl.domain.ConnectionProfileRepository
import com.thindie.rknzbl.domain.SettingsRepository

fun SettingsFlow.settings(
  repository: SettingsRepository,
  connectionProfileRepository: ConnectionProfileRepository,
) = RouteFactory.create(
  initialState = ScreenState(section = HomeSection.Settings),
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

      ScreenCommand.ToggleLocalStorage -> {
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

      ScreenCommand.ToggleCustomSource -> {
        go(source())
        null
      }

      ScreenCommand.OpenWebdav -> {
        go(webdav())
        null
      }

      ScreenCommand.OpenPerAppProxy -> {
        go(perAppMain())
        null
      }

      ScreenCommand.OpenHelp -> {
        openHelp()
        null
      }
    }
  },
  stateSink = { screenScope -> settingsStateSink(screenScope, repository) },
  id = "SettingsFlow-settings",
  section = HomeSection.Settings,
  routeContent = ::SettingsScreenContent,
)
