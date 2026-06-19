package com.thindie.rknzbl.feature.settings.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.thindie.rknzbl.engine.RouteFactory
import com.thindie.rknzbl.engine.ScreenScope
import com.thindie.rknzbl.feature.home.HomeFlow

/**
 * Settings screen for theme mode and autosave configuration.
 * Implements MVI pattern with reactive state management from storage.
 */
fun HomeFlow.settings() =
  RouteFactory.create(
    initialState = SettingsScreenState(),
    execute = { c: SettingsScreenCommand, s: SettingsScreenState ->
      when (c) {
        is SettingsScreenCommand.SetThemeMode -> {
          // Theme mode will be applied reactively via state sink
          s
        }

        is SettingsScreenCommand.ToggleAutosave -> {
          val newEnabled = !s.autosaveEnabled ?: true
          KeyValueStorage.setAutosaveEnabled(newEnabled)
          s.copy(autosaveEnabled = newEnabled)
        }

        SettingsScreenCommand.Back -> {
          finish(Unit)
          s
        }
      }
    },
    initialCommand = {
      // Load current theme mode on screen creation
      SettingsScreenCommand.SetThemeMode(KeyValueStorage.getThemeMode() ?: "auto")
    },
    stateSink = ::settingsStateSink,
    id = "HomeFlow-settings",
    routeContent = {
      AppScreen {
        BackHandler { send(SettingsScreenCommand.Back) }
        SettingsScreenContent(
          themeMode = state.themeMode ?: "auto",
          isAutosaveEnabled = state.autosaveEnabled ?: true,
          onThemeModeChange = { mode -> send(SettingsScreenCommand.SetThemeMode(mode)) },
          onToggleAutosave = { send(SettingsScreenCommand.ToggleAutosave) },
        )
      }
    },
  )
