package com.thindie.rknzbl.feature.perapp

import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.thindie.rknzbl.R
import com.thindie.rknzbl.engine.ScreenScope
import com.thindie.rknzbl.engine.ServiceCommand
import com.thindie.rknzbl.uikit.Action
import com.thindie.rknzbl.uikit.AppScreen
import com.thindie.rknzbl.uikit.AppTheme
import com.thindie.rknzbl.uikit.SentenceRow

@Composable
internal fun PerAppSearchScreen(scope: ScreenScope<SearchState, PerAppSearchCommand>) {
  val screenState by scope.state.collectAsState()
  val snackText = stringResource(R.string.per_app_proxy_added_snack)
  val focusRequester = remember { FocusRequester() }
  AppScreen(
    modifier = Modifier.imePadding(),
    primary =
      Action(
        resRef = R.drawable.ic_arrow_back_24,
        listener = { scope.send(PerAppSearchCommand.Back) },
      ),
  ) {
    BackHandler { scope.send(PerAppSearchCommand.Back) }
    val filtered =
      remember(screenState.searchQuery, screenState.allApps) {
        val q = screenState.searchQuery.trim().lowercase()
        if (q.isEmpty()) {
          emptyList()
        } else {
          screenState.allApps.filter { row ->
            row.appName.lowercase().contains(q) || row.packageName.lowercase().contains(q)
          }
        }
      }
    LazyColumn(
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      stickyHeader {
        Row(
          modifier =
            Modifier
              .fillMaxWidth()
              .background(AppTheme.colors.backgroundPrimary),
        ) {
          Column {
            Text(
              text = stringResource(R.string.per_app_proxy_search_screen_title),
              style = AppTheme.typography.headlineLarge,
              color = AppTheme.colors.contentPrimary,
            )
            Text(
              text = stringResource(R.string.per_app_proxy_search_screen_subtitle),
              style = AppTheme.typography.labelMedium,
              color = AppTheme.colors.contentSecondary,
            )
          }
        }
      }
      item {
        BasicTextField(
          modifier =
            Modifier
              .focusRequester(focusRequester)
              .fillMaxWidth()
              .background(AppTheme.colors.backgroundSecondary, shape = RoundedCornerShape(16.dp))
              .padding(16.dp),
          textStyle = TextStyle(color = AppTheme.colors.contentPrimary),
          value = screenState.searchQuery,
          onValueChange = { scope.send(PerAppSearchCommand.SetSearch(it)) },
        )
      }
      items(
        items = filtered.filter { it.packageName !in screenState.selectedPackages },
        key = { it.packageName },
      ) { row ->
        val appIcon = rememberAppIconPainter(row.packageName)
        SentenceRow(
          modifier =
            Modifier
              .border(
                border = BorderStroke(1.2.dp, AppTheme.colors.backgroundSecondary),
                shape = RoundedCornerShape(20.dp),
              )
              .fillMaxWidth(),
          painter = appIcon ?: painterResource(R.drawable.ic_internet_24),
          tintIcon = appIcon == null,
          title = row.appName,
          subtitle = row.packageName,
          loading = false,
          onClick = {
            scope.send(PerAppSearchCommand.AddPackage(row.packageName))
            scope.sendEvent(ServiceCommand.UiEvent.SnackText("$snackText ${row.appName}"))
          },
        )
      }
    }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    DisposableEffect(Unit) { onDispose { focusRequester.freeFocus() } }
  }
}

@Composable
internal fun rememberAppIconPainter(packageName: String): Painter? {
  val context = LocalContext.current
  val density = LocalDensity.current
  return remember(packageName, density) {
    try {
      val drawable = context.packageManager.getApplicationIcon(packageName)
      val px = with(density) { 40.dp.roundToPx() }.coerceAtLeast(1)
      BitmapPainter(drawable.toBitmap(px, px).asImageBitmap())
    } catch (_: PackageManager.NameNotFoundException) {
      null
    }
  }
}
