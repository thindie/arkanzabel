package com.thindie.rknzbl.feature.managegate.gatelist

import com.thindie.engine.core.ScreenScope
import com.thindie.engine.core.stateSink
import com.thindie.engine.core.sub
import com.thindie.engine.core.transition
import com.thindie.rknzbl.application.Application
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest

internal fun SelectSourceFlow.selectSourceStateSink(screenScope: ScreenScope<ScreenState, ScreenCommand>) {
  stateSink(screenScope) { s ->
    s.sub(
      (appContext as Application)
        .applicationScope
        .settingsRepositoryLegacy
        .isCustomSourceEnabled
        .filter { it }
        .flatMapLatest {
          appContext
            .applicationScope
            .settingsRepositoryLegacy
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
