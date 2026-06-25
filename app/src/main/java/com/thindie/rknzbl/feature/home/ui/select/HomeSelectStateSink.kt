package com.thindie.rknzbl.feature.home.ui.select

import com.thindie.rknzbl.engine.ScreenScope
import com.thindie.rknzbl.engine.stateSink
import com.thindie.rknzbl.engine.sub
import com.thindie.rknzbl.engine.transition
import com.thindie.rknzbl.feature.home.HomeFlow

internal fun HomeFlow.selectStateSink(screenScope: ScreenScope<ScreenState, ScreenCommand>) {
  screenScope.stateSink {
    sub(repository.autoSaved())
      .transition(
        block = { state, autosaved ->
          state.copy(autoSaved = autosaved)
        },
      )
  }
}
