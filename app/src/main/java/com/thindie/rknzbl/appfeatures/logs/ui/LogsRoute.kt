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
          if (s.rootTab == LogsRootTab.KERNEL) {
            LogSinkProvider.clearKernelByLevel(level)
            s.copy(
              kernelEntries =
                if (level == null) emptyList() else s.kernelEntries.filterNot { it.level == level },
            )
          } else {
            LogSinkProvider.clearByLevel(level)
            s.copy(
              debugEntries =
                if (level == null) emptyList() else s.debugEntries.filterNot { it.level == level },
            )
          }
        }

        is LogsScreenCommand.SetFilter -> s.copy(filterLevel = c.filter)

        is LogsScreenCommand.SetRootTab -> s.copy(rootTab = c.tab)
      }
    },
    section = HomeSection.Logs,
    stateSink = { screenScope ->
      screenScope.sub(LogSinkProvider.kernelEntries).transition { state, kernelEntries ->
        state.copy(kernelEntries = kernelEntries)
      }

      screenScope.sub(LogSinkProvider.entries).transition { state, debugEntries ->
        state.copy(debugEntries = debugEntries)
      }
    },
    routeContent = { LogsScreenContent(it) },
  )
