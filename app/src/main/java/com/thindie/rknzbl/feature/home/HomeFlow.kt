package com.thindie.rknzbl.feature.home

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.thindie.rknzbl.R
import com.thindie.rknzbl.application.Application
import com.thindie.rknzbl.engine.Command
import com.thindie.rknzbl.engine.Route
import com.thindie.rknzbl.engine.RouteFactory
import com.thindie.rknzbl.engine.Router
import com.thindie.rknzbl.engine.ScreenFlow
import com.thindie.rknzbl.engine.ScreenScope
import com.thindie.rknzbl.engine.WorkState
import com.thindie.rknzbl.feature.managegate.gatelist.SelectSourceFlow
import com.thindie.rknzbl.uikit.Action
import com.thindie.rknzbl.uikit.AppScreen
import com.thindie.rknzbl.uikit.AppTheme
import com.thindie.rknzbl.uikit.Button
import com.thindie.rknzbl.uikit.LocalThemeSwitcher
import com.thindie.rknzbl.uikit.SentenceRow
import com.thindie.rknzbl.uikit.ThemeSwitcher
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext

class HomeFlow(
  private val router: Router,
  private val appContext: Context,
) : ScreenFlow<Route, Unit>(router) {

  private var source: String =
    "https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/refs/heads/main/BLACK_VLESS_RUS_mobile.txt"

  override fun start() {
    router.push(main())
  }

  fun startSelectSourceFlow() {
    SelectSourceFlow(router)
      .onFinishBuilder { result ->
        when (result) {
          SelectSourceFlow.Result.FullBlackShadowSocks -> {
            source =
              "https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/refs/heads/main/BLACK_SS%2BAll_RUS.txt"
          }

          SelectSourceFlow.Result.FullBlackVless -> {
            source =
              "https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/refs/heads/main/BLACK_VLESS_RUS.txt"
          }

          SelectSourceFlow.Result.MobileBlackVless -> {
            source =
              "https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/refs/heads/main/BLACK_VLESS_RUS_mobile.txt"
          }

          SelectSourceFlow.Result.NotSelected -> Unit
          SelectSourceFlow.Result.WhiteListAll -> {
            source =
              "https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/refs/heads/main/WHITE-CIDR-RU-all.txt"
          }

          SelectSourceFlow.Result.WhiteListMobile -> {
            source =
              "https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/refs/heads/main/Vless-Reality-White-Lists-Rus-Mobile.txt"
          }

          SelectSourceFlow.Result.WhiteListMobileV2 -> {
            source =
              "https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/refs/heads/main/Vless-Reality-White-Lists-Rus-Mobile-2.txt"
          }

          SelectSourceFlow.Result.WhiteListRussian -> {
            source =
              "https://github.com/igareck/vpn-configs-for-russia/blob/main/WHITE-CIDR-RU-checked.txt"
          }
        }
      }
      .start()
  }

  fun main() = RouteFactory.create(
    initialState = State(),
    execute = ::exec,
    routeContent = { HomeScreen() },
    initialCommand = RouteFactory.InitialCommand {
      HomeCommand.Start as HomeCommand
    },
  )

  @Immutable
  data class State(
    val hint: String? = null,
    val links: List<ConnectionProfile> = emptyList(),
    val selected: ConnectionProfile? = null,
    val established: Boolean = false,
  ) : com.thindie.rknzbl.engine.State

  sealed interface HomeCommand : Command {
    data object Back : HomeCommand
    data object Start : HomeCommand
    data object Refresh : HomeCommand
    data object Stop : HomeCommand
    data object Choose : HomeCommand
    data class Select(val profile: ConnectionProfile) : HomeCommand

    data object Selected : HomeCommand
    data object TestLatency : HomeCommand

    data class NotifyConnection(val connection: String) : HomeCommand

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
          homeState.copy(selected = command.profile)
        }
      }


      HomeCommand.Start -> {
        withContext(Dispatchers.IO) {
          val links = HttpUtil.getUrlContent(
            url = source,
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
            url = source,
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

      HomeCommand.Selected -> {
        homeState.copy(established = true)
      }

      HomeCommand.TestLatency -> {
        if (V2RayServiceManager.isRunning()) {
          val (_, message) =
            SpeedtestManager.testConnection(appContext, SettingsManager.getHttpPort())
          homeState.copy(hint = message)
        } else {
          homeState
        }
      }

      is HomeCommand.NotifyConnection -> {
        homeState.copy(hint = command.connection)
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
    val application = LocalActivity.current?.application as Application
    LaunchedEffect(Unit) {
      application
        .vpnRuntimeState
        .onEach {
          when (it) {
            is WorkState.Error -> send(HomeFlow.HomeCommand.Dismissed)
            WorkState.Idle -> send(HomeFlow.HomeCommand.Dismissed)
            WorkState.Running -> {
              send(HomeFlow.HomeCommand.Selected)
              delay(1_500)
              send(HomeFlow.HomeCommand.TestLatency)
            }
          }
        }
        .launchIn(this)
    }

    PullToRefreshBox(
      isRefreshing = false,
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
            title = "Выбрать источник",
            subtitle = null,
            onClick = { send(HomeFlow.HomeCommand.Choose) },
            loading = false
          )
        }
        items(screenState.links) { item ->
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
            title = item.flow ?: item.server ?: item.serviceName ?: "",
            subtitle = item.remarks,
            loading = this@HomeScreen.processing.value is HomeFlow.HomeCommand.TestLatency && screenState.selected == item,
            onClick = { send(HomeFlow.HomeCommand.Select(item)) }
          )
        }
      }
      val st by state.collectAsState()
      Button(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(16.dp),
        enabled = st.established,
        text = if (st.established) "Остановить сервис" else "пока ничего не выбрано",
        onClick = { send(HomeFlow.HomeCommand.Stop) }
      )
    }
  }
}
