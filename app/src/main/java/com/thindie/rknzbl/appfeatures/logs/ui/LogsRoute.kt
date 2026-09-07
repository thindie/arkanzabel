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
          val level = c.filter?.level
          LogSinkProvider.clearByLevel(level)
          s.copy(
            entries = if (level == null) emptyList() else s.entries.filterNot { it.level == level },
          )
        }

        is LogsScreenCommand.SetFilter -> s.copy(filterLevel = c.filter)
      }
    },
    section = HomeSection.Logs,
    stateSink = { screenScope ->
      // Mirror the provider's full history into screen state on each emission
      screenScope.sub(LogSinkProvider.entries).transition { state, providerEntries ->
        val trimmed = if (providerEntries.size > 500) providerEntries.takeLast(500) else providerEntries
        state.copy(entries = trimmed)
      }
    },
    routeContent = { LogsScreenContent(it) },
  )
