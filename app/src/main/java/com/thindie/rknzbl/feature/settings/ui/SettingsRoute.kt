package com.thindie.rknzbl.feature.settings.ui

import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import com.thindie.rknzbl.engine.RouteFactory
import com.thindie.rknzbl.feature.home.HomeFlow
import com.thindie.rknzbl.feature.home.domain.ConnectionProfileRepository
import com.thindie.rknzbl.feature.settings.domain.SettingsRepository

fun HomeFlow.settings(
  repository: SettingsRepository,
  connectionProfileRepository: ConnectionProfileRepository,
) = RouteFactory.create(
  initialState = ScreenState(),
  execute = { c: ScreenCommand, s: ScreenState ->
    when (c) {
      is ScreenCommand.ToggleAutosave -> {
        val current = s.autosaveEnabled ?: true
        repository.toggleAutosave(!current)
        s.copy(autosaveEnabled = !current)
      }

      ScreenCommand.Back -> {
        back()
        s
      }

      is ScreenCommand.SelectLanguage -> {
        repository.setLanguage(c.languageCode)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          val localeManager = appContext.getSystemService(LocaleManager::class.java)
          localeManager.applicationLocales = LocaleList.forLanguageTags(c.languageCode)
          s
        } else {
          s.copy(legacyRestart = true)
        }
      }

      ScreenCommand.ToggleMux -> {
        val current = s.muxEnabled ?: false
        repository.toggleMux(!current)
        s.copy(muxEnabled = !current)
      }

      ScreenCommand.ToggleStorageMode -> {
        val current = s.isLocalSave ?: false
        repository.toggleLocalSave(!current)
        connectionProfileRepository.invalidateCaches()
        s.copy(isLocalSave = !current)
      }
    }
  },
  stateSink = { screenScope -> settingsStateSink(screenScope, repository) },
  id = "HomeFlow-settings",
  routeContent = {
    SettingsScreenContent()
  },
)
