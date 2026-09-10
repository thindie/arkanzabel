package com.thindie.rknzbl.appfeatures.logs.ui

import android.content.Context
import com.thindie.engine.core.RouteFactory
import com.thindie.engine.core.sub
import com.thindie.engine.core.transition
import com.thindie.rknzbl.appfeatures.home.HomeSection
import com.thindie.rknzbl.application.LogSinkProvider
import com.v2ray.ang.runtime.CoreLogFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

private const val KERNEL_LOG_POLL_MS = 2_000L
private const val MAX_KERNEL_ENTRIES = 500

/**
 * Factory: creates the logs-tab route for the bottom-nav design.
 */
@Suppress("FunctionName")
internal fun LogsRoute(appContext: Context) =
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
      // Kernel file logs are polled on a screen-scoped flow — cancelled with the route.
      val kernelFileEntries =
        flow {
          while (true) {
            emit(withContext(Dispatchers.IO) { CoreLogFiles.readEntries(appContext) })
            delay(KERNEL_LOG_POLL_MS)
          }
        }

      screenScope.sub(
        combine(LogSinkProvider.kernelEntries, kernelFileEntries) { mem, file ->
          (mem + file).sortedBy { it.timestamp }.takeLast(MAX_KERNEL_ENTRIES)
        },
      ).transition { state, entries ->
        state.copy(kernelEntries = entries)
      }

      screenScope.sub(LogSinkProvider.entries).transition { state, debugEntries ->
        state.copy(debugEntries = debugEntries)
      }
    },
    routeContent = { LogsScreenContent(it) },
  )
