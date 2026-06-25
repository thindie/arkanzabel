package com.thindie.rknzbl.feature.managegate.gatelist

import com.thindie.rknzbl.application.Application
import com.thindie.rknzbl.engine.ScreenScope
import com.thindie.rknzbl.engine.stateSink
import com.thindie.rknzbl.engine.sub
import com.thindie.rknzbl.engine.transition
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest

internal fun SelectSourceFlow.selectSourceStateSink(screenScope: ScreenScope<ScreenState, ScreenCommand>) {
  screenScope.stateSink {
    sub(
      (appContext as Application)
        .applicationScope
        .settings
        .repository
        .isCustomSourceEnabled
        .filter { it }
        .flatMapLatest {
          appContext
            .applicationScope
            .settings
            .repository
            .customSourceUrl
            .filterNotNull()
        }
        .distinctUntilChanged(),
    ).transition { state, url ->
      val sections = (state.blackSection + SelectSourceFlow.Result.CustomSource(url)).toSet()
      state.copy(blackSection = sections.toList())
    }
  }
}
