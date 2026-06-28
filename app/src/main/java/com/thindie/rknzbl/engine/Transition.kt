package com.thindie.rknzbl.engine

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

fun <S : State, C : Command> stateSink(
  scope: ScreenScope<S, C>,
  block: (ScreenScope<S, C>) -> Unit,
) {
  block(scope)
}

fun <S : State, C : Command, R : Any?> ScreenScope<S, C>.sub(flow: Flow<R>): Pair<ScreenScope<S, C>, Flow<R>> {
  return this to flow
}

fun <S : State, C : Command, R : Any?> Pair<ScreenScope<S, C>, Flow<R>>.transition(
  action: suspend (S, S, R) -> Unit = { _, _, _ -> },
  block: suspend (S, R) -> S = { s, _ -> s },
): ScreenScope<S, C> {
  val (screenScope, flow) = this
  screenScope.scope?.let { scope ->
    flow
      .onEach { any ->
        val current = screenScope.state.value
        val newState = block(current, any)
        if (newState != current) {
          action(current, newState, any)
          screenScope.update(newState)
        }
      }
      .launchIn(scope)
  }
  return screenScope
}
