package com.thindie.rknzbl.appfeatures.settings.ui

import com.thindie.engine.core.ScreenScope
import com.thindie.engine.core.sub
import com.thindie.engine.core.transition
import com.thindie.rknzbl.domain.SettingsRepository

internal fun settingsStateSink(
  screenScope: ScreenScope<ScreenState, ScreenCommand>,
  repository: SettingsRepository,
) {
  screenScope.sub(repository.autosaveEnabled)
    .transition { state, enabled ->
      val newState = state.copy(autosaveEnabled = enabled)
      newState
    }

  screenScope.sub(repository.muxEnabled)
    .transition { state, enabled ->
      state.copy(muxEnabled = enabled)
    }

  screenScope.sub(repository.language)
    .transition { state, lang ->
      state.copy(language = lang)
    }

  screenScope.sub(repository.isLocalSave)
    .transition { state, enabled ->
      state.copy(isLocalSave = enabled)
    }

  screenScope.sub(repository.speedEnabled)
    .transition { state, enabled ->
      state.copy(speedEnabled = enabled)
    }

  screenScope.sub(repository.customSourceUrl)
    .transition { state, url ->
      val trimmed = url?.trim()
      state.copy(customSourceUrl = if (trimmed.isNullOrBlank()) null else trimmed)
    }

  screenScope.sub(repository.webDavConfig)
    .transition { state, config ->
      state.copy(webDavConfig = config)
    }
}
