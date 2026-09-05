package com.thindie.rknzbl.appfeatures.logs.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.thindie.engine.core.Log
import com.thindie.engine.core.LogEntry
import com.thindie.engine.core.ScreenScope
import com.thindie.engine.uikit.AppScreen
import com.thindie.engine.uikit.AppTheme
import com.thindie.engine.uikit.TabItem
import com.thindie.engine.uikit.TabRow
import com.thindie.engine.uikit.VSpacer
import com.thindie.rknzbl.R
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
internal fun LogsScreenContent(scope: ScreenScope<LogsScreenState, LogsScreenCommand>) {
  val st by scope.state.collectAsState()

  AppScreen(scope) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = stringResource(R.string.logs_title),
        style = AppTheme.typography.headlineLarge,
        color = AppTheme.colors.contentPrimary,
      )
      VSpacer(8.dp)

      // Filter tabs
      TabRow(
        items = filterTabs(),
        selected = st.filterLevel?.ordinal ?: 0,
        onTabSelected = { index ->
          val filters = LogFilter.entries
          if (index < filters.size) {
            scope.send(LogsScreenCommand.SetFilter(filters[index]))
          } else {
            scope.send(LogsScreenCommand.SetFilter(null))
          }
        },
      ) { _ ->
        VSpacer(8.dp)

        // Clear button
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End) {
          Button(onClick = { scope.send(LogsScreenCommand.ClearLogs) }) {
            Text(stringResource(R.string.logs_clear))
          }
        }

        VSpacer(8.dp)

        // Log entries list
        val filteredEntries = filterEntries(st.entries, st.filterLevel)

        if (filteredEntries.isEmpty()) {
          Text(
            text = stringResource(R.string.logs_empty),
            modifier = Modifier.fillMaxWidth(),
            color = AppTheme.colors.contentSecondary,
          )
        } else {
          LazyColumn {
            itemsIndexed(filteredEntries) { index, entry ->
              LogEntryRow(entry)
              if (index < filteredEntries.size - 1) {
                VSpacer(4.dp)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun filterTabs(): List<TabItem> {
  return LogFilter.entries.map { f -> TabItem(stringResource(f.labelRes)) }
}

private fun filterEntries(
  entries: List<LogEntry>,
  filter: LogFilter?,
): List<LogEntry> {
  if (filter == null) return entries
  return when (filter) {
    LogFilter.ALL -> entries
    LogFilter.ERROR -> entries.filter { it.level == Log.Level.ERROR }
    LogFilter.WARN -> entries.filter { it.level == Log.Level.WARN }
    LogFilter.INFO -> entries.filter { it.level == Log.Level.INFO }
    LogFilter.DEBUG -> entries.filter { it.level == Log.Level.DEBUG }
  }
}

@Composable
private fun LogEntryRow(entry: LogEntry) {
  val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.Top,
  ) {
    // Timestamp
    Text(
      text = timeFormat.format(entry.timestamp),
      style = AppTheme.typography.bodySmall,
      color = AppTheme.colors.contentSecondary,
      modifier = Modifier.padding(end = 8.dp),
    )

    // Level badge
    Text(
      text = entry.level.tag,
      style = AppTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
      color = levelColor(entry.level),
      modifier = Modifier.padding(end = 8.dp),
    )

    // Tag and message
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = entry.tag,
        style = AppTheme.typography.bodySmall,
        color = AppTheme.colors.contentSecondary,
      )
      Text(
        text = entry.message,
        style = AppTheme.typography.bodyMedium,
        color = AppTheme.colors.contentPrimary,
      )
    }
  }
}

@Composable
private fun levelColor(level: Log.Level): androidx.compose.ui.graphics.Color {
  return when (level) {
    Log.Level.ERROR -> AppTheme.colors.errorPrimary
    Log.Level.WARN -> AppTheme.colors.accentPrimary
    Log.Level.INFO -> AppTheme.colors.successPrimary
    else -> AppTheme.colors.contentSecondary
  }
}
