package com.thindie.rknzbl.feature.settings.ui

import com.thindie.engine.core.ScreenScope
import com.thindie.engine.core.sub
import com.thindie.engine.core.transition
import com.thindie.rknzbl.feature.home.HomeFlow
import com.thindie.rknzbl.feature.settings.domain.SettingsRepository
import kotlinx.coroutines.flow.filterNotNull
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

  screenScope.sub(repository.fragmentEnabled)
    .transition { state, enabled ->
      state.copy(fragmentEnabled = enabled)
    }

  screenScope.sub(repository.muxEnabled)
    .transition { state, enabled ->
      state.copy(muxEnabled = enabled)
    }

  screenScope.sub(repository.fragmentInterval.mapNotNull { it?.ifBlank { null } })
    .transition { state, interval ->
      state.copy(fragmentInterval = interval)
    }

  // Language - read from storage on subscription start
  screenScope.sub(repository.language)
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

  // Force profile measurement support
  screenScope.sub(repository.forceProfileMeasure)
    .transition { state, enabled ->
      state.copy(forceProfileMeasure = enabled)
    }

  screenScope.sub(repository.customSourceUrl.mapNotNull { it?.ifBlank { null } })
    .transition { state, source ->
      state.copy(customSourceUrl = source)
    }

  // Reality masquerade (show) support
  screenScope.sub(repository.realityShowEnabled)
    .transition { state, enabled ->
      state.copy(realityShowEnabled = enabled)
    }

  // Sniffing target protocol support
  screenScope.sub(repository.sniffingTarget().filterNotNull())
    .transition { state, target ->
      state.copy(sniffingTarget = target)
    }

  // Sniffing port-range support
  screenScope.sub(repository.sniffingPortRange().filterNotNull())
    .transition { state, range ->
      state.copy(sniffingPortRange = range)
    }
}
