package com.thindie.engine.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

fun <S : ViewState, C : Command> stateSink(
  scope: ScreenScope<S, C>,
  block: (ScreenScope<S, C>) -> Unit,
) {
  block(scope)
}

fun <S : ViewState, C : Command, R : Any?> ScreenScope<S, C>.sub(flow: Flow<R>): Pair<ScreenScope<S, C>, Flow<R>> {
  return this to flow
}

fun <S : ViewState, C : Command, R : Any?> Pair<ScreenScope<S, C>, Flow<R>>.transition(
  action: suspend (S, S, R) -> Unit = { _, _, _ -> },
  block: suspend (S, R) -> S = { s, _ -> s },
) {
  val (screenScope, flow) = this
  screenScope.scope?.launch {
    flow.collect { any ->
      val (current, newState) = screenScope.updateState { state -> block(state, any) }
      if (newState != current) {
        action(current, newState, any)
      }
    }
  }
}
