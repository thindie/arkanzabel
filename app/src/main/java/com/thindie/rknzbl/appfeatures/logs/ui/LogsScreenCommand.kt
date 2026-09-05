package com.thindie.rknzbl.appfeatures.logs.ui

import com.thindie.engine.core.Command

sealed interface LogsScreenCommand : Command {
  data object ClearLogs : LogsScreenCommand

  data class SetFilter(val filter: LogFilter?) : LogsScreenCommand
}
