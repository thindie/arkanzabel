package com.thindie.rknzbl.appfeatures.logs.ui

import com.thindie.engine.core.RouteFactory
import com.thindie.engine.core.sub
import com.thindie.engine.core.transition
import com.thindie.rknzbl.appfeatures.home.HomeSection
import com.thindie.rknzbl.application.LogSinkProvider

/**
 * Factory: creates the logs-tab route for the bottom-nav design.
 */
@Suppress("FunctionName")
internal fun LogsRoute() =
  RouteFactory.create(
    id = "LogsFlow-logs",
    initialState = LogsScreenState(),
    execute = { c: LogsScreenCommand, s: LogsScreenState ->
      when (c) {
        is LogsScreenCommand.ClearLogs -> {
          LogSinkProvider.clear()
          s.copy(entries = emptyList())
        }

        is LogsScreenCommand.SetFilter -> s.copy(filterLevel = c.filter)
      }
    },
    section = HomeSection.Logs,
    stateSink = { screenScope ->
      // Receive log entries from the sink provider
      screenScope.sub(LogSinkProvider.entries).transition { state, newEntries ->
        val combined = state.entries + newEntries
        // Keep last 500 entries
        if (combined.size > 500) {
          state.copy(entries = combined.takeLast(500))
        } else {
          state.copy(entries = combined)
        }
      }
    },
    routeContent = { LogsScreenContent(it) },
  )
