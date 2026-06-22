package com.thindie.rknzbl.feature.settings.ui

import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import com.thindie.rknzbl.engine.RouteFactory
import com.thindie.rknzbl.feature.home.HomeFlow
import com.thindie.rknzbl.feature.home.domain.ConnectionProfileRepository
import com.thindie.rknzbl.feature.settings.domain.SettingsRepository
import com.thindie.rknzbl.feature.settings.ui.inputurl.createInputUrl

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

      ScreenCommand.StartWithFavoriteProfiles -> {
        val current = s.startWithFavoriteProfiles ?: false
        repository.toggleStartWithFavoriteProfiles(!current)
        s.copy(startWithFavoriteProfiles = !current)
      }

      ScreenCommand.ToggleSpeed -> {
        val current = s.speedEnabled ?: false
        repository.toggleSpeed(!current)
        s.copy(speedEnabled = !current)
      }

      ScreenCommand.ToggleCustomSource -> {
        val current = s.isCustomSourceEnabled
        if (current) {
          repository.setCustomSourceEnabled(false)
          s.copy(isCustomSourceEnabled = false, customSourceUrl = null)
        } else {
          repository.setCustomSourceEnabled(true)
          go(createInputUrl())
          s.copy(isCustomSourceEnabled = true)
        }
      }

      is ScreenCommand.SetCustomSourceUrl -> {
        repository.setCustomSourceUrl(c.url)
        s.copy(customSourceUrl = c.url)
      }
    }
  },
  stateSink = { screenScope -> settingsStateSink(screenScope, repository) },
  id = "HomeFlow-settings",
  routeContent = {
    SettingsScreenContent()
  },
)
