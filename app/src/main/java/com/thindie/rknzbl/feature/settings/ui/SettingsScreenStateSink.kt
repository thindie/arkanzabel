package com.thindie.rknzbl.feature.settings.ui

import com.thindie.rknzbl.engine.ScreenScope
import com.thindie.rknzbl.engine.sub
import com.thindie.rknzbl.engine.transition
import com.thindie.rknzbl.feature.home.HomeFlow
import com.thindie.rknzbl.feature.settings.domain.SettingsRepository
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull

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

  // Language - read from storage on subscription start
  screenScope.sub(flow { emit(repository.language()) })
    .transition { state, lang ->
      state.copy(language = lang)
    }

  // Local storage mode support
  screenScope.sub(repository.isLocalSave)
    .transition { state, enabled ->
      state.copy(isLocalSave = enabled)
    }

  // Start with favorite profiles support
  screenScope.sub(repository.startWithFavoriteProfiles)
    .transition { state, enabled ->
      state.copy(startWithFavoriteProfiles = enabled)
    }

  // Speed notification support
  screenScope.sub(repository.speedEnabled)
    .transition { state, enabled ->
      state.copy(speedEnabled = enabled)
    }

  screenScope.sub(repository.isCustomSourceEnabled)
    .transition { state, enabled ->
      state.copy(isCustomSourceEnabled = enabled)
    }

  screenScope.sub(repository.customSourceUrl.mapNotNull { it?.ifBlank { null } })
    .transition { state, source ->
      state.copy(customSourceUrl = source)
    }
}
