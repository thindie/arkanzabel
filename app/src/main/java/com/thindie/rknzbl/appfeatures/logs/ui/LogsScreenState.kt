package com.thindie.rknzbl.appfeatures.logs.ui

import androidx.compose.runtime.Immutable
import com.thindie.engine.core.Log
import com.thindie.engine.core.LogEntry
import com.thindie.engine.core.ViewState

@Immutable
internal data class LogsScreenState(
  val entries: List<LogEntry> = emptyList(),
  val filterLevel: LogFilter? = null,
) : ViewState

enum class LogFilter(val labelRes: Int) {
  ALL(com.thindie.rknzbl.R.string.logs_filter_all),
  ERROR(com.thindie.rknzbl.R.string.logs_filter_error),
  WARN(com.thindie.rknzbl.R.string.logs_filter_warn),
  INFO(com.thindie.rknzbl.R.string.logs_filter_info),
  DEBUG(com.thindie.rknzbl.R.string.logs_filter_debug),
}

val LogFilter.level: Log.Level?
  get() =
    when (this) {
      LogFilter.ALL -> null
      LogFilter.ERROR -> Log.Level.ERROR
      LogFilter.WARN -> Log.Level.WARN
      LogFilter.INFO -> Log.Level.INFO
      LogFilter.DEBUG -> Log.Level.DEBUG
    }
