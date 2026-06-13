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
import androidx.compose.foundation.layout.Column
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
import com.thindie.rknzbl.engine.ServiceCommand
import com.thindie.rknzbl.engine.WorkState
import com.thindie.rknzbl.engine.stateSink
import com.thindie.rknzbl.engine.sub
import com.thindie.rknzbl.engine.transition
import com.thindie.rknzbl.feature.home.domain.ConnectionProfileRepository
import com.thindie.rknzbl.feature.managegate.gatelist.SelectSourceFlow
import com.thindie.rknzbl.feature.managegate.gatelist.resolveLabels
import com.thindie.rknzbl.feature.perapp.PerAppProxyFlow
import com.thindie.rknzbl.feature.managegate.storedgates.FavoriteProfilesFlow
import com.thindie.rknzbl.uikit.Action
import com.thindie.rknzbl.uikit.AppScreen
import com.thindie.rknzbl.uikit.AppTheme
import com.thindie.rknzbl.uikit.Button
import com.thindie.rknzbl.uikit.CircularProgress
import com.thindie.rknzbl.uikit.HSpacer
import com.thindie.rknzbl.uikit.LocalThemeSwitcher
import com.thindie.rknzbl.uikit.SentenceRow
import com.thindie.rknzbl.uikit.ThemeSwitcher
import com.thindie.rknzbl.uikit.VSpacer
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID


class HomeFlow(
  private val router: Router,
  private val appContext: Context,
  private val repository: ConnectionProfileRepository,
) : ScreenFlow<Route, Unit>(router) {

  private val sourceChanges = MutableSharedFlow<SelectSourceFlow.Result>(
    extraBufferCapacity = 1,
    onBufferOverflow = BufferOverflow.DROP_OLDEST,
  )

  override fun start() {
    go(select())
  }

  fun startSelectSourceFlow() {
    SelectSourceFlow(router, appContext)
      .onFinishBuilder { result -> sourceChanges.tryEmit(result) }
      .start()
  }

  fun startStoredProfilesFlow(onFinish: () -> Unit) {
    FavoriteProfilesFlow(
      router = router,
      repository = repository,
      appContext = appContext
    )
      .onFinishBuilder { onFinish.invoke() }
      .start()
  }

  fun startPerAppProxyFlow() {
    PerAppProxyFlow(router = router, appContext = appContext).start()
  }

  fun stateSink(screenScope: ScreenScope<State, HomeCommand>) {
    screenScope.stateSink {
      sub(sourceChanges)
        .transition(
          action = { _, _, source ->
            when (source) {
              SelectSourceFlow.Result.FullBlackShadowSocks,
              SelectSourceFlow.Result.FullBlackVless,
              SelectSourceFlow.Result.MobileBlackVless,
              SelectSourceFlow.Result.NotSelected,
              SelectSourceFlow.Result.WhiteListAll,
              SelectSourceFlow.Result.WhiteListMobile,
              SelectSourceFlow.Result.WhiteListMobileV2,
              SelectSourceFlow.Result.WhiteListRussian,
                -> send(HomeCommand.Refresh)

              SelectSourceFlow.Result.StoredProfiles -> {
                startStoredProfilesFlow { go(main()) }
              }
            }
          }
        ) { s, source ->
          val newState =
            s.copy(
              sourceName = source.resolveLabels(appContext).second,
              sourceUrl = source.sourceUrl.orEmpty()
            )
          newState
        }

      screenScope.stateSink {
        sub(
          (appContext as Application).vpnRuntimeState.map { state ->
            when (state) {
              is WorkState.Error -> state to null
              WorkState.Idle -> state to null
              WorkState.Running -> {
                val port = screenScope.state.value.selected?.serverPort
                if (port != null) {
                  state to SpeedtestManager.testConnection(
                    appContext,
                    SettingsManager.getHttpPort()
                  )
                } else state to null
              }
            }
          }
        )
          .transition({ _, _, (vpnState, speedTestMessage) ->
          when (vpnState) {
            is WorkState.Error -> send(HomeCommand.Dismissed)
            WorkState.Idle -> Unit
            WorkState.Running -> {
              if (speedTestMessage != null) {
                sendEvent(ServiceCommand.UiEvent.SnackText(speedTestMessage))
              }
            }
          }
        }) { s, (vpnState, _) ->
          s.copy(
            serviceBeingStarted = when (vpnState) {
              is WorkState.Error -> null
              WorkState.Idle -> true
              WorkState.Running -> null
            }.takeIf { s.serviceBeingStarted != null },
            established = when (vpnState) {
              is WorkState.Error -> false
              WorkState.Idle -> false
              WorkState.Running -> true
            }
          )
        }
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
    id = "Home-main",
    initialCommand = RouteFactory.InitialCommand {
      HomeCommand.Start as HomeCommand
    },
  )

  private object HomeSelect : com.thindie.rknzbl.engine.State
  private sealed interface HomeSelectCommand : Command {
    data object Home : HomeSelectCommand
    data object New : HomeSelectCommand
    data object Settings : HomeSelectCommand
    data object Back : HomeSelectCommand
  }

  fun select() = RouteFactory.create(
    initialState = HomeSelect,
    execute = { c: HomeSelectCommand, s: HomeSelect ->
      when (c) {
        HomeSelectCommand.Home -> {
          startStoredProfilesFlow { }
        }

        HomeSelectCommand.New -> {
          go(main())
        }

        HomeSelectCommand.Settings -> {
          startPerAppProxyFlow()
        }
        HomeSelectCommand.Back -> {
          finish(Unit)
        }
      }
      s
    },
    id = "HomeFlow-select",
    routeContent = {
      AppScreen {
        BackHandler { send(HomeSelectCommand.Back) }
        Column(modifier = Modifier
          .fillMaxSize()
          .padding(16.dp)) {
          Row(modifier = Modifier.fillMaxWidth()) {
            Text(
              text = stringResource(R.string.app_name),
              style = AppTheme.typography.headlineLarge,
              color = AppTheme.colors.contentPrimary
            )
          }
          Text(
            text = stringResource(R.string.home_select_tagline),
            style = AppTheme.typography.labelMedium,
            color = AppTheme.colors.contentSecondary
          )
          VSpacer(24.dp)
          SentenceRow(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(R.string.home_select_new_profiles_title),
            subtitle = stringResource(R.string.home_select_new_profiles_subtitle),
            painter = painterResource(R.drawable.ic_internet_24),
            onClick = { send(HomeSelectCommand.New) },
            loading = false,
          )
          VSpacer(16.dp)
          SentenceRow(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(R.string.home_select_stored_title),
            subtitle = stringResource(R.string.home_select_stored_subtitle),
            painter = painterResource(R.drawable.ic_home_24),
            onClick = { send(HomeSelectCommand.Home) },
            loading = false,
          )
          VSpacer(16.dp)
          SentenceRow(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(R.string.home_select_vpn_modes_title),
            subtitle = stringResource(R.string.home_select_vpn_modes_subtitle),
            painter = painterResource(R.drawable.ic_settings_24),
            onClick = { send(HomeSelectCommand.Settings) },
            loading = false,
          )
        }
      }
    }
  )

  @Immutable
  data class State(
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
    data class Save(val profile: ConnectionProfile) : HomeCommand

    data object Dismissed : HomeCommand
    data object OpenPerAppProxy : HomeCommand
  }

  private suspend fun exec(command: HomeCommand, homeState: State): State {

    return when (command) {
      is HomeCommand.Back -> {
        back()
        homeState
      }

      is HomeCommand.Select -> {
        withContext(Dispatchers.Default) {
          V2RayServiceManager.startVService(
            context = appContext,
            guid = KeyValueStorage.encodeServerConfig(
              guid = UUID.randomUUID().toString(),
              config = command.profile
            )
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
        homeState.copy(selected = null, established = false)
      }

      HomeCommand.Choose -> {
        startSelectSourceFlow()
        homeState
      }

      is HomeCommand.Save -> {
        val guid = KeyValueStorage.getSelectServer()
        repository.save(requireNotNull(guid))
        homeState
      }

      HomeCommand.OpenPerAppProxy -> {
        startPerAppProxyFlow()
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
    secondary = Action(
      resRef = R.drawable.ic_theme_24,
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
        stickyHeader {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(AppTheme.colors.backgroundPrimary)
          ) {
            Text(
              text = stringResource(R.string.home_downloaded_profiles_header),
              style = AppTheme.typography.headlineLarge,
              color = AppTheme.colors.contentPrimary
            )
          }
        }
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
        item {
          SentenceRow(
            painter = painterResource(R.drawable.ic_information_24),
            title = stringResource(R.string.per_app_proxy_row_title),
            subtitle = stringResource(R.string.per_app_proxy_row_subtitle),
            onClick = { send(HomeFlow.HomeCommand.OpenPerAppProxy) },
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
                ), shape = RoundedCornerShape(20.dp)
              )
              .fillMaxWidth(),
            painter = painterResource(R.drawable.ic_internet_24),
            title = item.remarks + item.serverPort.orEmpty(),
            subtitle = item.flow ?: item.server ?: item.serviceName ?: "",
            loading = false,
            onClick = { send(HomeFlow.HomeCommand.Select(item)) },
            onLongClick = if (st.selected == item) {
              {
                sendEvent(
                  ServiceCommand.UiEvent.Decision(
                    content = {
                      Text(
                        text = stringResource(R.string.source_stored_add),
                        style = AppTheme.typography.bodyMedium
                      )
                    },
                    primaryAction = Action(
                      resRef = R.string.source_select_done,
                      listener = { send(HomeFlow.HomeCommand.Save(item)) }
                    )
                  )
                )
              }
            } else null
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
