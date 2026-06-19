package com.thindie.rknzbl.feature.settings.ui

import androidx.compose.runtime.Immutable
import com.thindie.rknzbl.engine.ScreenScope
import com.thindie.rknzbl.engine.stateSink
import com.thindie.rknzbl.engine.sub
import com.thindie.rknzbl.feature.home.HomeFlow
import com.v2ray.ang.runtime.KeyValueStorage

/**
 * Reactive state sink for the Settings Screen (MVI Pattern).
 * Subscribes to flows and updates state accordingly following MVI pattern.
 */
fun HomeFlow.settingsStateSink(screenScope: ScreenScope<SettingsScreenState, SettingsScreenCommand>) {
  screenScope.stateSink {
    // Listen for theme mode changes from storage
    sub(KeyValueStorage.getThemeMode().asFlow()) { state, _, themeMode ->
      val newState =
        state.copy(
          themeMode = themeMode ?: "auto",
        )
      newState
    }

    // Listen for autosave enabled changes from storage
    sub(KeyValueStorage.isAutosaveEnabled().asFlow()) { state, _, isAutosaveEnabled ->
      val newState =
        state.copy(
          autosaveEnabled = isAutosaveEnabled,
        )
      newState
    }
  }
}
