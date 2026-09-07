package com.thindie.rknzbl.appfeatures.logs.ui

import com.thindie.engine.core.Command

sealed interface LogsScreenCommand : Command {
  data class ClearLogs(val filter: LogFilter? = null) : LogsScreenCommand

  data class SetFilter(val filter: LogFilter?) : LogsScreenCommand
}
