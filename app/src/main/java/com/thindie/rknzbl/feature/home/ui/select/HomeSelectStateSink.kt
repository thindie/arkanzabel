package com.thindie.rknzbl.feature.home.ui.select

import com.thindie.engine.core.ScreenScope
import com.thindie.engine.core.stateSink
import com.thindie.engine.core.sub
import com.thindie.engine.core.transition
import com.thindie.rknzbl.feature.home.HomeFlow

internal fun HomeFlow.selectStateSink(screenScope: ScreenScope<ScreenState, ScreenCommand>) {
  stateSink(screenScope) { s ->
    s.sub(repository.autoSaved())
      .transition(
        block = { state, autosaved ->
          state.copy(autoSaved = autosaved)
        },
      )
  }
}
