package com.thindie.rknzbl.feature.managegate.gatelist

import com.thindie.engine.core.Command

sealed interface ScreenCommand : Command {
  data object Back : ScreenCommand

  data class Go(val type: SelectSourceFlow.Result) : ScreenCommand
}
