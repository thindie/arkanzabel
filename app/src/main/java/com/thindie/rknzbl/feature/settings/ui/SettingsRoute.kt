package com.thindie.rknzbl.feature.settings.ui

import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import com.thindie.engine.core.RouteFactory
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
        null
      }

      ScreenCommand.Back -> {
        back()
        null
      }

      is ScreenCommand.SelectLanguage -> {
        repository.setLanguage(c.languageCode)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          val localeManager = appContext.getSystemService(LocaleManager::class.java)
          localeManager.applicationLocales = LocaleList.forLanguageTags(c.languageCode)
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

      ScreenCommand.ToggleFragment -> {
        val current = s.fragmentEnabled ?: false
        repository.toggleFragment(!current)
        null
      }

      is ScreenCommand.SetFragmentInterval -> {
        repository.setFragmentInterval(c.interval)
        null
      }

      ScreenCommand.ToggleStorageMode -> {
        val current = s.isLocalSave ?: false
        repository.toggleLocalSave(!current)
        connectionProfileRepository.invalidateCaches()
        null
      }

      ScreenCommand.StartWithFavoriteProfiles -> {
        val current = s.startWithFavoriteProfiles ?: false
        repository.toggleStartWithFavoriteProfiles(!current)
        null
      }

      ScreenCommand.ToggleSpeed -> {
        val current = s.speedEnabled ?: false
        repository.toggleSpeed(!current)
        null
      }

      ScreenCommand.ToggleCustomSource -> {
        val current = s.isCustomSourceEnabled
        if (current) {
          repository.setCustomSourceEnabled(false)
          s.copy(customSourceUrl = null)
        } else {
          repository.setCustomSourceEnabled(true)
          go(createInputUrl())
          null
        }
      }

      is ScreenCommand.SetCustomSourceUrl -> {
        repository.setCustomSourceUrl(c.url)
        null
      }

      ScreenCommand.ToggleRealityShow -> {
        val current = s.realityShowEnabled ?: false
        repository.toggleRealityShow(!current)
        null
      }

      is ScreenCommand.SetSniffingTarget -> {
        repository.setSniffingTarget(c.target)
        null
      }

      is ScreenCommand.SetSniffingPortRange -> {
        repository.setSniffingPortRange(c.range)
        null
      }
    }
  },
  stateSink = { screenScope -> settingsStateSink(screenScope, repository) },
  id = "HomeFlow-settings",
  routeContent = ::SettingsScreenContent,
)
