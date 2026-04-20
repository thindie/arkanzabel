package com.thindie.rknzbl.feature.managegate.storedgates.profiles

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.thindie.rknzbl.R
import com.thindie.rknzbl.application.Application
import com.thindie.rknzbl.engine.Command
import com.thindie.rknzbl.engine.RouteFactory
import com.thindie.rknzbl.engine.ScreenScope
import com.thindie.rknzbl.engine.ServiceCommand
import com.thindie.rknzbl.engine.WorkState
import com.thindie.rknzbl.engine.stateSink
import com.thindie.rknzbl.engine.sub
import com.thindie.rknzbl.engine.transition
import com.thindie.rknzbl.feature.managegate.storedgates.FavoriteProfilesFlow
import com.thindie.rknzbl.uikit.Action
import com.thindie.rknzbl.uikit.AppScreen
import com.thindie.rknzbl.uikit.AppTheme
import com.thindie.rknzbl.uikit.Button
import com.thindie.rknzbl.uikit.CircularProgress
import com.thindie.rknzbl.uikit.HSpacer
import com.thindie.rknzbl.uikit.SentenceRow
import com.thindie.rknzbl.uikit.VSpacer
import com.thindie.rknzbl.uikit.surface
import com.v2ray.ang.dto.ConnectionProfile
import com.v2ray.ang.runtime.KeyValueStorage
import com.v2ray.ang.runtime.SettingsManager
import com.v2ray.ang.runtime.SpeedtestManager
import com.v2ray.ang.runtime.V2RayServiceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

fun FavoriteProfilesFlow.profiles() = RouteFactory.create(
  initialState = ScreenState(),
  execute = ::exec,
  stateSink = ::stateSink,
  routeContent = { RouteScreen() },
  errorMapper = ::errorMapper,
  initialCommand = RouteFactory.InitialCommand {
    ScreenCommand.RequestStoredProfiles
  })


data class ScreenState(
  val profiles: List<ConnectionProfile> = emptyList(),
  val selected: ConnectionProfile? = null,
  val established: Boolean = false,
  val serviceBeingStarted: Boolean? = null,
) : com.thindie.rknzbl.engine.State

sealed interface ScreenCommand : Command {
  data object BackRequested : ScreenCommand
  data object RequestStoredProfiles : ScreenCommand

  data object StopService : ScreenCommand
  data object ServiceError : ScreenCommand
  data class Select(val profile: ConnectionProfile) : ScreenCommand
  data class Delete(val profile: ConnectionProfile) : ScreenCommand
}

private fun FavoriteProfilesFlow.stateSink(screenScope: ScreenScope<ScreenState, ScreenCommand>) {
  screenScope.stateSink {
    sub(
      (appContext as Application).vpnRuntimeState.map { state ->
        when (state) {
          is WorkState.Error -> state to null
          WorkState.Idle -> state to null
          WorkState.Running -> {
            val port = screenScope.state.value.selected?.serverPort
            if (port != null) {
              state to SpeedtestManager.testConnection(appContext, SettingsManager.getHttpPort())
            } else state to null
          }
        }
      }).transition({ _, _, (vpnState, latency) ->
      when (vpnState) {
        is WorkState.Error -> send(ScreenCommand.ServiceError)
        WorkState.Idle -> Unit
        WorkState.Running -> {
          if (latency != null) {
            sendEvent(ServiceCommand.UiEvent.SnackText(latency.second))
          }
        }
      }
    }) { s, (vpnState, _) ->
      s.copy(
        serviceBeingStarted = when (vpnState) {
          is WorkState.Error -> null
          WorkState.Idle -> true
          WorkState.Running -> null
        }.takeIf { s.serviceBeingStarted != null }, established = when (vpnState) {
          is WorkState.Error -> false
          WorkState.Idle -> false
          WorkState.Running -> true
        }
      )
    }
  }
}

private suspend fun FavoriteProfilesFlow.exec(c: ScreenCommand, s: ScreenState): ScreenState {
  return when (c) {
    ScreenCommand.BackRequested -> {
      finish(Unit)
      s
    }

    ScreenCommand.RequestStoredProfiles -> {
      withContext(Dispatchers.IO) {
        val parsed = repository.read()
        s.copy(profiles = parsed)
      }
    }

    is ScreenCommand.Delete -> {
      withContext(Dispatchers.IO) {
        if (c.profile == s.selected) {
          repository.delete(c.profile)
          V2RayServiceManager.stopVService(appContext)
          val profiles = repository.read()
          s.copy(
            profiles = profiles,
            selected = null
          )
        } else {
          val selected = s.selected
          repository.delete(c.profile)
          val profiles = repository.read()
          s.copy(
            selected = selected,
            profiles = profiles
          )
        }
      }
    }

    is ScreenCommand.Select -> {
      withContext(Dispatchers.Default) {
        V2RayServiceManager.startVService(
          context = appContext, guid = KeyValueStorage.encodeServerConfig(
            guid = UUID.randomUUID().toString(), config = c.profile
          )
        )
        s.copy(selected = c.profile, serviceBeingStarted = true)
      }
    }

    ScreenCommand.StopService -> {
      V2RayServiceManager.stopVService(appContext)
      s
    }

    ScreenCommand.ServiceError -> {
      s.copy(selected = null, established = false)
    }
  }
}

@Composable
private fun ScreenScope<ScreenState, ScreenCommand>.RouteScreen() {

  val screenState by state.collectAsState()
  AppScreen(
    primary = Action(
      resRef = R.drawable.ic_arrow_back_24, listener = {
        send(command = ScreenCommand.BackRequested)
      })
  ) {
    BackHandler { send(ScreenCommand.BackRequested) }
    val st by state.collectAsState()
    val height = LocalWindowInfo.current.containerSize.height.dp
    PullToRefreshBox(
      isRefreshing = false, modifier = Modifier.height(height), onRefresh = {
        send(ScreenCommand.RequestStoredProfiles)
      }) {
      LazyColumn(
        contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        stickyHeader {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(AppTheme.colors.backgroundPrimary)
          ) {
            Text(
              text = stringResource(R.string.source_stored),
              style = AppTheme.typography.headlineLarge,
              color = AppTheme.colors.contentPrimary,
            )
          }
        }
        items(
          items = screenState.profiles,
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
            title = item.remarks + " " + item.serverPort.orEmpty(),
            subtitle = item.flow ?: item.server ?: item.serviceName ?: "",
            loading = false,
            onClick = { send(ScreenCommand.Select(item)) },
            onLongClick = if (st.selected == item) {
              {
                sendEvent(
                  ServiceCommand.UiEvent.Decision(
                    content = {
                      Aware(
                        painter = painterResource(R.drawable.ic_close_16),
                        title = stringResource(R.string.source_stored_delete),
                        subtitle = stringResource(R.string.source_stored_delete_subtitle)
                      )
                    }, primaryAction = Action(
                      resRef = R.string.source_select_done,
                      listener = { send(ScreenCommand.Delete(item)) })
                  )
                )
              }
            } else null)
        }
      }
      Button(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(16.dp),
        enabled = st.established || screenState.profiles.isEmpty(),
        text = when {
          this@RouteScreen.processing.value is ScreenCommand.Select -> ""
          screenState.profiles.isEmpty() -> stringResource(R.string.home_fetch_profiles)
          st.established -> stringResource(R.string.home_stop_service)
          else -> stringResource(R.string.home_pick_profile_first)
        },
        onClick = {
          if (st.established) {
            send(ScreenCommand.StopService)
          } else {

          }
        })
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
            color = AppTheme.colors.backgroundSecondary, shape = RoundedCornerShape(20.dp)
          )
          .padding(16.dp)
      )
      AnimatedVisibility(
        modifier = Modifier
          .fillMaxWidth()
          .padding(
            all = 16.dp
          ), visible = true
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(
              all = 16.dp
            )
            .surface(
              backgroundColor = AppTheme.colors.backgroundPrimary, shape = RoundedCornerShape(20.dp)
            )
            .height(56.dp), verticalAlignment = Alignment.CenterVertically
        ) {
          HSpacer(16.dp)
          Text(
            stringResource(R.string.home_starting_vpn), style = AppTheme.typography.bodyMedium
          )
        }
      }
    }
  }
}

@Composable
private fun Aware(
  modifier: Modifier = Modifier,
  painter: Painter,
  title: String,
  subtitle: String?,
) {
  Row(
    modifier = modifier
      .background(
        color = AppTheme.colors.backgroundSecondary, shape = RoundedCornerShape(20.dp)
      )
      .clip(shape = RoundedCornerShape(20.dp))
      .padding(horizontal = 16.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(contentAlignment = Alignment.Center) {
      Icon(
        painter = painter, contentDescription = null, modifier = Modifier
          .background(
            color = AppTheme.colors.backgroundSecondary, shape = RoundedCornerShape(20.dp)
          )
          .padding(8.dp)
          .size(32.dp), tint = AppTheme.colors.errorPrimary
      )
    }
    HSpacer(12.dp)
    if (subtitle != null) {
      Column {
        Text(
          text = title,
          style = AppTheme.typography.titleMedium,
          color = AppTheme.colors.contentPrimary
        )
        VSpacer(2.dp)
        Text(
          text = subtitle,
          style = AppTheme.typography.bodyMedium,
          color = AppTheme.colors.contentSecondary
        )
      }
    } else {
      Text(
        text = title,
        style = AppTheme.typography.titleMedium,
        color = AppTheme.colors.contentPrimary
      )
    }
  }
}