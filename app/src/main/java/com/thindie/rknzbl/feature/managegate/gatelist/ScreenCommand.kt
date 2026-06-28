package com.thindie.rknzbl.feature.managegate.gatelist

import com.thindie.rknzbl.engine.Command

sealed interface ScreenCommand : Command {
  data object Back : ScreenCommand

  data class Select(val type: SelectSourceFlow.Result) : ScreenCommand
}
