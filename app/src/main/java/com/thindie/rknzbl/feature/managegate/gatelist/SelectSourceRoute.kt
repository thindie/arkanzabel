package com.thindie.rknzbl.feature.managegate.gatelist

import com.thindie.rknzbl.R
import com.thindie.rknzbl.engine.RouteFactory
import com.thindie.rknzbl.engine.ScreenScopeError

fun SelectSourceFlow.main() =
  RouteFactory.create(
    initialState = ScreenState(),
    execute = ::exec,
    routeContent = { SelectSourceScreen() },
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
): ScreenState {
  return when (command) {
    is ScreenCommand.Back -> {
      finish(state.selected)
      state
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
