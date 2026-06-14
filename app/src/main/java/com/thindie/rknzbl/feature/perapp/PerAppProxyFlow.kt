package com.thindie.rknzbl.feature.perapp

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
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
import com.thindie.rknzbl.engine.Command
import com.thindie.rknzbl.engine.Route
import com.thindie.rknzbl.engine.RouteFactory
import com.thindie.rknzbl.engine.Router
import com.thindie.rknzbl.engine.ScreenFlow
import com.thindie.rknzbl.engine.ScreenScope
import com.thindie.rknzbl.engine.ServiceCommand
import com.thindie.rknzbl.uikit.Action
import com.thindie.rknzbl.uikit.AppScreen
import com.thindie.rknzbl.uikit.AppTheme
import com.thindie.rknzbl.uikit.SentenceRow
import com.thindie.rknzbl.uikit.surface
import com.v2ray.ang.AppConfig
import com.v2ray.ang.runtime.KeyValueStorage
import com.v2ray.ang.util.AppManagerUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PerAppProxyFlow(
  private val router: Router,
  private val appContext: Context,
) : ScreenFlow<Route, Unit>(router) {
  override fun start() {
    go(main())
  }

  enum class ProxyScopeMode {
    All,
    Selected,
  }

  @Immutable
  data class AppRow(
    val appName: String,
    val packageName: String,
  )

  @Immutable
  data class State(
    val mode: ProxyScopeMode,
    val allApps: List<AppRow>,
    val selectedPackages: Set<String>,
  ) : com.thindie.rknzbl.engine.State

  @Immutable
  data class SearchState(
    val searchQuery: String,
    val allApps: List<AppRow>,
    val selectedPackages: Set<String>,
  ) : com.thindie.rknzbl.engine.State

  sealed interface PerAppProxyCommand : Command {
    data object Back : PerAppProxyCommand

    data object LoadApps : PerAppProxyCommand

    data object RefreshFromStorage : PerAppProxyCommand

    data object SetModeAll : PerAppProxyCommand

    data object SetModeSelected : PerAppProxyCommand

    data object OpenSearch : PerAppProxyCommand

    data class RemovePackage(val packageName: String) : PerAppProxyCommand
  }

  sealed interface PerAppSearchCommand : Command {
    data object Back : PerAppSearchCommand

    data object LoadApps : PerAppSearchCommand

    data class SetSearch(val query: String) : PerAppSearchCommand

    data class AddPackage(val packageName: String) : PerAppSearchCommand
  }

  private fun persistModeAll() {
    KeyValueStorage.encodeSettings(AppConfig.PREF_PER_APP_PROXY, false)
  }

  private fun persistModeSelected() {
    KeyValueStorage.encodeSettings(AppConfig.PREF_PER_APP_PROXY, true)
    KeyValueStorage.encodeSettings(AppConfig.PREF_BYPASS_APPS, false)
  }

  private fun persistPackages(set: Set<String>) {
    KeyValueStorage.encodeSettings(AppConfig.PREF_PER_APP_PROXY_SET, set)
  }

  fun main() =
    RouteFactory.create(
      initialState =
        State(
          mode =
            if (KeyValueStorage.decodeSettingsBool(AppConfig.PREF_PER_APP_PROXY)) {
              ProxyScopeMode.Selected
            } else {
              ProxyScopeMode.All
            },
          allApps = emptyList(),
          selectedPackages =
            KeyValueStorage.decodeSettingsStringSet(AppConfig.PREF_PER_APP_PROXY_SET)
              ?: emptySet(),
        ),
      execute = ::execMain,
      routeContent = { PerAppProxyScreen() },
      id = "PerApp-main",
      initialCommand =
        RouteFactory.InitialCommand {
          PerAppProxyCommand.LoadApps as PerAppProxyCommand
        },
    )

  fun search() =
    RouteFactory.create(
      initialState =
        SearchState(
          searchQuery = "",
          allApps = emptyList(),
          selectedPackages =
            KeyValueStorage.decodeSettingsStringSet(AppConfig.PREF_PER_APP_PROXY_SET)
              ?: emptySet(),
        ),
      execute = ::execSearch,
      routeContent = { PerAppSearchScreen() },
      id = "Perapp-id",
      initialCommand =
        RouteFactory.InitialCommand {
          PerAppSearchCommand.LoadApps as PerAppSearchCommand
        },
    )

  private suspend fun execMain(
    command: PerAppProxyCommand,
    state: State,
  ): State {
    return when (command) {
      PerAppProxyCommand.Back -> {
        finish(Unit)
        state
      }

      PerAppProxyCommand.LoadApps -> {
        val rows =
          withContext(Dispatchers.IO) {
            AppManagerUtil.loadAppsForPerAppTunneling(appContext)
              .map { AppRow(it.appName, it.packageName) }
              .sortedBy { it.appName.lowercase() }
          }
        state.copy(allApps = rows)
      }

      PerAppProxyCommand.RefreshFromStorage ->
        state.copy(
          mode =
            if (KeyValueStorage.decodeSettingsBool(AppConfig.PREF_PER_APP_PROXY)) {
              ProxyScopeMode.Selected
            } else {
              ProxyScopeMode.All
            },
          selectedPackages =
            KeyValueStorage.decodeSettingsStringSet(AppConfig.PREF_PER_APP_PROXY_SET)
              ?: emptySet(),
        )

      PerAppProxyCommand.SetModeAll -> {
        persistModeAll()
        state.copy(mode = ProxyScopeMode.All)
      }

      PerAppProxyCommand.SetModeSelected -> {
        persistModeSelected()
        state.copy(mode = ProxyScopeMode.Selected)
      }

      PerAppProxyCommand.OpenSearch -> {
        go(search())
        state
      }

      is PerAppProxyCommand.RemovePackage -> {
        val next = state.selectedPackages - command.packageName
        persistPackages(next)
        state.copy(selectedPackages = next)
      }
    }
  }

  private suspend fun execSearch(
    command: PerAppSearchCommand,
    state: SearchState,
  ): SearchState {
    return when (command) {
      PerAppSearchCommand.Back -> {
        back()
        state
      }

      PerAppSearchCommand.LoadApps -> {
        val rows =
          withContext(Dispatchers.IO) {
            AppManagerUtil.loadAppsForPerAppTunneling(appContext)
              .map { AppRow(it.appName, it.packageName) }
              .sortedBy { it.appName.lowercase() }
          }
        val sel = KeyValueStorage.decodeSettingsStringSet(AppConfig.PREF_PER_APP_PROXY_SET) ?: emptySet()
        state.copy(allApps = rows, selectedPackages = sel)
      }

      is PerAppSearchCommand.SetSearch -> {
        state.copy(searchQuery = command.query)
      }

      is PerAppSearchCommand.AddPackage -> {
        val next = state.selectedPackages + command.packageName
        persistPackages(next)
        state.copy(selectedPackages = next, searchQuery = "")
      }
    }
  }
}

@Composable
private fun ScreenScope<PerAppProxyFlow.State, PerAppProxyFlow.PerAppProxyCommand>.PerAppProxyScreen() {
  val screenState by state.collectAsState()
  LaunchedEffect(Unit) {
    send(PerAppProxyFlow.PerAppProxyCommand.RefreshFromStorage)
  }
  AppScreen(
    modifier = Modifier.imePadding(),
    title = null,
    primary =
      Action(
        resRef = R.drawable.ic_arrow_back_24,
        listener = { send(PerAppProxyFlow.PerAppProxyCommand.Back) },
      ),
  ) {
    BackHandler { send(PerAppProxyFlow.PerAppProxyCommand.Back) }
    val appsByPackage =
      remember(screenState.allApps) {
        screenState.allApps.associateBy { it.packageName }
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
              text = stringResource(R.string.per_app_proxy_title),
              style = AppTheme.typography.headlineLarge,
              color = AppTheme.colors.contentPrimary,
            )
            Text(
              text = stringResource(R.string.per_app_proxy_mode_section),
              style = AppTheme.typography.labelMedium,
              color = AppTheme.colors.contentSecondary,
            )
          }
        }
      }
      item {
        SentenceRow(
          modifier =
            Modifier
              .border(
                border =
                  BorderStroke(
                    width = 1.2.dp,
                    color =
                      if (screenState.mode == PerAppProxyFlow.ProxyScopeMode.All) {
                        AppTheme.colors.accentPrimary
                      } else {
                        AppTheme.colors.backgroundSecondary
                      },
                  ),
                shape = RoundedCornerShape(20.dp),
              )
              .fillMaxWidth(),
          painter = painterResource(R.drawable.ic_internet_24),
          title = stringResource(R.string.per_app_proxy_mode_all),
          subtitle = stringResource(R.string.per_app_proxy_mode_all_subtitle),
          loading = false,
          onClick = { send(PerAppProxyFlow.PerAppProxyCommand.SetModeAll) },
        )
      }
      item {
        SentenceRow(
          modifier =
            Modifier
              .border(
                border =
                  BorderStroke(
                    width = 1.2.dp,
                    color =
                      if (screenState.mode == PerAppProxyFlow.ProxyScopeMode.Selected) {
                        AppTheme.colors.accentPrimary
                      } else {
                        AppTheme.colors.backgroundSecondary
                      },
                  ),
                shape = RoundedCornerShape(20.dp),
              )
              .fillMaxWidth(),
          painter = painterResource(R.drawable.ic_information_24),
          title = stringResource(R.string.per_app_proxy_mode_selected),
          subtitle = stringResource(R.string.per_app_proxy_mode_selected_subtitle),
          loading = false,
          onClick = { send(PerAppProxyFlow.PerAppProxyCommand.SetModeSelected) },
        )
      }
      if (screenState.mode == PerAppProxyFlow.ProxyScopeMode.Selected) {
        stickyHeader {
          Row(
            modifier =
              Modifier
                .fillMaxWidth()
                .background(AppTheme.colors.backgroundPrimary),
          ) {
            Column {
              Text(
                text = stringResource(R.string.per_app_proxy_applied_section),
                style = AppTheme.typography.headlineLarge,
                color = AppTheme.colors.contentPrimary,
              )
              val description =
                if (screenState.selectedPackages.isEmpty()) {
                  stringResource(R.string.per_app_proxy_none_selected)
                } else {
                  stringResource(R.string.per_app_proxy_mode_selected_header_subtitle)
                }
              Text(
                text = description,
                style = AppTheme.typography.labelMedium,
                color = AppTheme.colors.contentSecondary,
              )
            }
          }
        }
        items(
          items = screenState.selectedPackages.sorted(),
          key = { it },
        ) { pkg ->
          val row = appsByPackage[pkg]
          val title = row?.appName ?: pkg
          val subtitle = row?.packageName ?: pkg
          val fallbackPainter = painterResource(R.drawable.ic_internet_24)
          val appIcon = rememberAppIconPainter(pkg)
          SentenceRow(
            modifier =
              Modifier
                .border(
                  border =
                    BorderStroke(
                      width = 1.2.dp,
                      color = AppTheme.colors.backgroundSecondary,
                    ),
                  shape = RoundedCornerShape(20.dp),
                )
                .fillMaxWidth(),
            painter = appIcon ?: fallbackPainter,
            tintIcon = appIcon == null,
            title = title,
            subtitle = subtitle,
            loading = false,
            onClick = {
              send(PerAppProxyFlow.PerAppProxyCommand.RemovePackage(pkg))
            },
          )
        }
        stickyHeader {
          Row(
            modifier =
              Modifier
                .fillMaxWidth()
                .background(AppTheme.colors.backgroundPrimary),
          ) {
            Column {
              Text(
                text = stringResource(R.string.per_app_proxy_open_search_title),
                style = AppTheme.typography.headlineLarge,
                color = AppTheme.colors.contentPrimary,
              )
              Text(
                text = stringResource(R.string.per_app_proxy_open_search_subtitle),
                style = AppTheme.typography.labelMedium,
                color = AppTheme.colors.contentSecondary,
              )
            }
          }
        }
        item {
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .height(52.dp)
                .surface(
                  shape = RoundedCornerShape(16.dp),
                  onClick = {
                    send(PerAppProxyFlow.PerAppProxyCommand.OpenSearch)
                  },
                  backgroundColor = AppTheme.colors.backgroundSecondary,
                ),
          )
        }
      }
    }
  }
}

@Composable
private fun ScreenScope<PerAppProxyFlow.SearchState, PerAppProxyFlow.PerAppSearchCommand>.PerAppSearchScreen() {
  val screenState by state.collectAsState()
  val snackText = stringResource(R.string.per_app_proxy_added_snack)
  val focusRequester = remember { FocusRequester() }
  AppScreen(
    modifier = Modifier.imePadding(),
    primary =
      Action(
        resRef = R.drawable.ic_arrow_back_24,
        listener = { send(PerAppProxyFlow.PerAppSearchCommand.Back) },
      ),
  ) {
    BackHandler { send(PerAppProxyFlow.PerAppSearchCommand.Back) }
    val filtered =
      remember(screenState.searchQuery, screenState.allApps) {
        val q = screenState.searchQuery.trim().lowercase()
        if (q.isEmpty()) {
          emptyList()
        } else {
          screenState.allApps.filter { row ->
            row.appName.lowercase().contains(q) ||
              row.packageName.lowercase().contains(q)
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
              .background(
                AppTheme.colors.backgroundSecondary,
                shape = RoundedCornerShape(16.dp),
              )
              .padding(16.dp),
          textStyle =
            TextStyle(
              color = AppTheme.colors.contentPrimary,
            ),
          value = screenState.searchQuery,
          onValueChange = { send(PerAppProxyFlow.PerAppSearchCommand.SetSearch(it)) },
        )
      }
      items(
        items = filtered.filter { it.packageName !in screenState.selectedPackages },
        key = { it.packageName },
      ) { row ->
        val fallbackPainter = painterResource(R.drawable.ic_internet_24)
        val appIcon = rememberAppIconPainter(row.packageName)
        SentenceRow(
          modifier =
            Modifier
              .border(
                border =
                  BorderStroke(
                    width = 1.2.dp,
                    color = AppTheme.colors.backgroundSecondary,
                  ),
                shape = RoundedCornerShape(20.dp),
              )
              .fillMaxWidth(),
          painter = appIcon ?: fallbackPainter,
          tintIcon = appIcon == null,
          title = row.appName,
          subtitle = row.packageName,
          loading = false,
          onClick = {
            send(PerAppProxyFlow.PerAppSearchCommand.AddPackage(row.packageName))
            sendEvent(
              ServiceCommand.UiEvent.SnackText(
                snackText + " ${row.appName}",
              ),
            )
          },
        )
      }
    }
    LaunchedEffect(Unit) {
      focusRequester.requestFocus()
    }
    DisposableEffect(Unit) {
      onDispose { focusRequester.freeFocus() }
    }
  }
}

@Composable
private fun rememberAppIconPainter(packageName: String): Painter? {
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
