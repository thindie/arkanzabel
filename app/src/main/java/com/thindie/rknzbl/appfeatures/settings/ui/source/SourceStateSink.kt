package com.thindie.rknzbl.appfeatures.settings.ui.source

import com.thindie.engine.core.ScreenScope
import com.thindie.engine.core.sub
import com.thindie.engine.core.transition
import com.thindie.rknzbl.domain.SettingsRepository
import kotlinx.coroutines.flow.mapNotNull

internal fun sourceStateSink(
  screenScope: ScreenScope<SourceState, SourceCommand>,
  repository: SettingsRepository,
) {
  screenScope.sub(repository.customSourceUrl.mapNotNull { it?.ifBlank { null } })
    .transition { state, url ->
      state.copy(currentUrl = url, customUrlInput = url ?: "")
    }
}
