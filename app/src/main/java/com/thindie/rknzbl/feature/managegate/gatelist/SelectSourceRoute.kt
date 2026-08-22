package com.thindie.rknzbl.feature.managegate.gatelist

import com.thindie.engine.core.RouteFactory
import com.thindie.engine.core.ScreenScopeError
import com.thindie.rknzbl.R

fun SelectSourceFlow.main() =
  RouteFactory.create(
    initialState = ScreenState(),
    execute = ::exec,
    routeContent = ::SelectSourceScreen,
    id = "Select-main",
    errorMapper = {
      ScreenScopeError(
        message = appContext.getString(R.string.error_unexpected),
        actions = emptyMap(),
      )
    },
    stateSink = ::selectSourceStateSink,
  )

private suspend fun SelectSourceFlow.exec(
  command: ScreenCommand,
  state: ScreenState,
): ScreenState? {
  return when (command) {
    is ScreenCommand.Back -> {
      finish(state.selected)
      null
    }

    is ScreenCommand.Select -> {
      if (state.selected == command.type) {
        state.copy(selected = SelectSourceFlow.Result.NotSelected)
      } else {
        state.copy(selected = command.type)
      }
    }
  }
}
