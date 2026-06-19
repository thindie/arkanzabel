package com.thindie.rknzbl.feature.settings.ui

import com.thindie.rknzbl.engine.ScreenScope
import com.thindie.rknzbl.engine.sub
import com.thindie.rknzbl.engine.transition
import com.thindie.rknzbl.feature.home.HomeFlow
import com.thindie.rknzbl.feature.settings.domain.SettingsRepository

internal fun HomeFlow.settingsStateSink(
  screenScope: ScreenScope<ScreenState, ScreenCommand>,
  repository: SettingsRepository,
) {
  // Autosave support
  screenScope.sub(repository.autosaveEnabled)
    .transition { state, enabled ->
      val newState = state.copy(autosaveEnabled = enabled)
      newState
    }

  // MUX support
  screenScope.sub(repository.muxEnabled)
    .transition { state, enabled ->
      state.copy(muxEnabled = enabled)
    }
}
