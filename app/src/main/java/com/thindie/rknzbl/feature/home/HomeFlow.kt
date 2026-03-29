package com.thindie.rknzbl.feature.home

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.thindie.rknzbl.R
import com.thindie.rknzbl.application.Application
import com.thindie.rknzbl.engine.Command
import com.thindie.rknzbl.engine.Route
import com.thindie.rknzbl.engine.RouteFactory
import com.thindie.rknzbl.engine.Router
import com.thindie.rknzbl.engine.ScreenFlow
import com.thindie.rknzbl.engine.ScreenScope
import com.thindie.rknzbl.engine.ScreenScopeError
import com.thindie.rknzbl.engine.WorkState
import com.thindie.rknzbl.engine.stateSink
import com.thindie.rknzbl.engine.sub
import com.thindie.rknzbl.engine.transition
import com.thindie.rknzbl.feature.managegate.gatelist.SelectSourceFlow
import com.thindie.rknzbl.feature.managegate.gatelist.resolveLabels
import com.thindie.rknzbl.uikit.Action
import com.thindie.rknzbl.uikit.AppScreen
import com.thindie.rknzbl.uikit.AppTheme
import com.thindie.rknzbl.uikit.Button
import com.thindie.rknzbl.uikit.CircularProgress
import com.thindie.rknzbl.uikit.HSpacer
import com.thindie.rknzbl.uikit.LocalThemeSwitcher
import com.thindie.rknzbl.uikit.SentenceRow
import com.thindie.rknzbl.uikit.ThemeSwitcher
import com.thindie.rknzbl.uikit.surface
import com.v2ray.ang.dto.ConnectionProfile
import com.v2ray.ang.runtime.KeyValueStorage
import com.v2ray.ang.runtime.ProfileUriParser
import com.v2ray.ang.runtime.SettingsManager
import com.v2ray.ang.runtime.SpeedtestManager
import com.v2ray.ang.runtime.V2RayServiceManager
import com.v2ray.ang.util.HttpUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext


class HomeFlow(
  private val router: Router,
  private val appContext: Context,
) : ScreenFlow<Route, Unit>(router) {

  private val sourceChanges = MutableSharedFlow<SelectSourceFlow.Result>(
    extraBufferCapacity = 1,
    onBufferOverflow = BufferOverflow.DROP_OLDEST,
  )

  override fun start() {
    router.push(main())
  }

  fun startSelectSourceFlow() {
    SelectSourceFlow(router, appContext)
      .onFinishBuilder { result -> sourceChanges.tryEmit(result) }
      .start()
  }

  fun stateSink(screenScope: ScreenScope<State, HomeCommand>) {
    screenScope.stateSink {
      sub(sourceChanges)
        .transition(
          action = { _, _, _ -> send(HomeCommand.Refresh) }
        ) { s, source ->
          val newState =
            s.copy(
              sourceName = source.resolveLabels(appContext).second,
              sourceUrl = source.sourceUrl.orEmpty()
            )
          newState
        }

      sub((appContext as Application).vpnRuntimeState)
        .transition(
          { _, _, vpnState ->
            when (vpnState) {
              is WorkState.Error -> send(HomeCommand.Dismissed)
              WorkState.Idle -> Unit
              WorkState.Running -> send(HomeCommand.TestLatency)
            }
          }
        ) { s, vpnState ->
          s.copy(
            serviceBeingStarted = when (vpnState) {
              is WorkState.Error -> null
              WorkState.Idle -> true
              WorkState.Running -> null
            }
              .takeIf { s.serviceBeingStarted != null },
            established = when (vpnState) {
              is WorkState.Error -> false
              WorkState.Idle -> false
              WorkState.Running -> true
            }
          )
        }
    }
  }

  fun main() = RouteFactory.create(
    initialState = State(
      sourceName = SelectSourceFlow.Result.MobileBlackVless.resolveLabels(appContext).first,
      sourceUrl = SelectSourceFlow.Result.MobileBlackVless.sourceUrl,
    ),
    execute = ::exec,
    stateSink = ::stateSink,
    routeContent = { HomeScreen() },
    errorMapper = {
      ScreenScopeError(
        message = appContext.getString(R.string.error_unexpected),
        actions = emptyMap(),
      )
    },
    initialCommand = RouteFactory.InitialCommand {
      HomeCommand.Start as HomeCommand
    },
  )

  @Immutable
  data class State(
    val hint: String? = null,
    val sourceName: String,
    val sourceUrl: String,
    val links: List<ConnectionProfile> = emptyList(),
    val selected: ConnectionProfile? = null,
    val established: Boolean = false,
    val serviceBeingStarted: Boolean? = null,
  ) : com.thindie.rknzbl.engine.State

  sealed interface HomeCommand : Command {
    data object Back : HomeCommand
    data object Start : HomeCommand
    data object Refresh : HomeCommand
    data object Stop : HomeCommand
    data object Choose : HomeCommand
    data class Select(val profile: ConnectionProfile) : HomeCommand
    data object TestLatency : HomeCommand

    data object Dismissed : HomeCommand
  }

  private suspend fun exec(command: HomeCommand, homeState: State): State {

    return when (command) {
      is HomeCommand.Back -> {
        finish(Unit)
        homeState
      }

      is HomeCommand.Select -> {
        withContext(Dispatchers.Default) {
          V2RayServiceManager.startVService(
            context = appContext,
            guid = KeyValueStorage.encodeServerConfig("", command.profile)
          )
          homeState.copy(selected = command.profile, serviceBeingStarted = true)
        }
      }

      HomeCommand.Start -> {
        withContext(Dispatchers.IO) {
          val links = HttpUtil.getUrlContent(
            url = homeState.sourceUrl,
            timeout = 10_000
          )
          val parsed = links?.split("\n")
            ?.map { uri ->
              async { ProfileUriParser.parse(uri) }
            }
            ?.awaitAll()
            ?.mapNotNull { it }
          homeState.copy(links = parsed.orEmpty())
        }
      }

      HomeCommand.Stop -> {
        V2RayServiceManager.stopVService(appContext)
        homeState
      }

      HomeCommand.Refresh -> {
        withContext(Dispatchers.IO) {
          val links = HttpUtil.getUrlContent(
            url = homeState.sourceUrl,
            timeout = 10_000
          )
          val parsed = links?.split("\n")
            ?.map { uri ->
              async { ProfileUriParser.parse(uri) }
            }
            ?.awaitAll()
            ?.mapNotNull { it }
          homeState.copy(links = parsed ?: homeState.links)
        }
      }

      HomeCommand.Dismissed -> {
        homeState.copy(selected = null, established = false, hint = null)
      }

      HomeCommand.TestLatency -> {
        val (_, message) =
          SpeedtestManager.testConnection(appContext, SettingsManager.getHttpPort())

        homeState.copy(hint = message)
      }

      HomeCommand.Choose -> {
        startSelectSourceFlow()
        homeState
      }
    }
  }
}


@Composable
fun ScreenScope<HomeFlow.State, HomeFlow.HomeCommand>.HomeScreen() {
  val themeSwitcher = LocalThemeSwitcher.current
  val themeColors = LocalThemeSwitcher.current.themeFlow.collectAsState(null)
  val isDark = when (themeColors.value) {
    null -> isSystemInDarkTheme()
    ThemeSwitcher.Choice.Dark -> true
    ThemeSwitcher.Choice.Light -> false
    ThemeSwitcher.Choice.Auto -> isSystemInDarkTheme()
  }
  val screenState by state.collectAsState()
  AppScreen(
    subtitle = screenState.hint,
    secondary = Action(
      icon = R.drawable.ic_shield_extra_24,
      listener = {
        themeSwitcher.set(
          if (isDark) ThemeSwitcher.Choice.Light else ThemeSwitcher.Choice.Dark
        )
      }
    )
  ) {
    BackHandler { send(HomeFlow.HomeCommand.Back) }
    val st by state.collectAsState()
    val height = LocalWindowInfo.current.containerSize.height.dp
    PullToRefreshBox(
      isRefreshing = false,
      modifier = Modifier.height(height),
      onRefresh = {
        send(HomeFlow.HomeCommand.Refresh)
      }
    ) {
      LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        item {
          SentenceRow(
            painter = painterResource(R.drawable.ic_chevron_right_24),
            title = if (st.sourceName.isBlank()) {
              stringResource(R.string.home_choose_source)
            } else {
              stringResource(R.string.home_source_selected_prefix)
            },
            subtitle = st.sourceName,
            onClick = { send(HomeFlow.HomeCommand.Choose) },
            loading = false
          )
        }
        items(
          items = screenState.links,
        ) { item ->
          SentenceRow(
            modifier = Modifier
              .border(
                border = BorderStroke(
                  width = 1.2.dp,
                  color = if (screenState.selected == item && screenState.established) {
                    AppTheme.colors.accentPrimary
                  } else AppTheme.colors.backgroundSecondary
                ),
                shape = RoundedCornerShape(20.dp)
              )
              .fillMaxWidth(),
            painter = painterResource(R.drawable.ic_internet_24),
            title = item.remarks,
            subtitle = item.flow ?: item.server ?: item.serviceName ?: "",
            loading = false,
            onClick = { send(HomeFlow.HomeCommand.Select(item)) },
          )
        }
      }
      Button(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(16.dp),
        enabled = st.established || st.links.isEmpty(),
        text = when {
          this@HomeScreen.processing == HomeFlow.HomeCommand.Start -> ""
          st.links.isEmpty() -> stringResource(R.string.home_fetch_profiles)
          st.established -> stringResource(R.string.home_stop_service)
          else -> stringResource(R.string.home_pick_profile_first)
        },
        onClick = {
          if (st.established) {
            send(HomeFlow.HomeCommand.Stop)
          } else {
            send(HomeFlow.HomeCommand.Start)
          }
        }
      )
    }
  }
  if (screenState.serviceBeingStarted == true) {
    Box(
      Modifier
        .fillMaxSize()
        .background(
          Color.Transparent.copy(alpha = 0.3f)
        )
        .clickable(onClick = {}, enabled = false)
    ) {
      CircularProgress(
        modifier = Modifier
          .align(Alignment.Center)
          .background(
            color = AppTheme.colors.backgroundSecondary,
            shape = RoundedCornerShape(20.dp)
          )
          .padding(16.dp)
      )
      AnimatedVisibility(
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(
              all = 16.dp
            ), visible = true
      ) {
        Row(
          modifier =
            Modifier
              .fillMaxWidth()
              .padding(
                all = 16.dp
              )
              .surface(
                backgroundColor = AppTheme.colors.backgroundPrimary,
                shape = RoundedCornerShape(20.dp)
              )
              .height(56.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          HSpacer(16.dp)
          Text(
            stringResource(R.string.home_starting_vpn),
            style = AppTheme.typography.bodyMedium
          )
        }
      }
    }
  }
}
