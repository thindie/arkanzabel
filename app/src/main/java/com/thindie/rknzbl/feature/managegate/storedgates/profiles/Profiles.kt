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
import kotlinx.coroutines.withContext
import java.util.UUID

fun FavoriteProfilesFlow.profiles() =
  RouteFactory.create(
    initialState = ScreenState(),
    execute = ::exec,
    stateSink = ::stateSink,
    routeContent = { RouteScreen() },
    errorMapper = ::errorMapper,
    id = "profiles",
  )

data class ScreenState(
  val profiles: List<ConnectionProfile> = emptyList(),
  val selected: ConnectionProfile? = null,
  val selectedTestConnectionMessage: SpeedtestManager.SpeedTestResult? = null,
  val selectedProfiles: Set<ConnectionProfile> = emptySet(),
  val selectionMode: Boolean = false,
  val established: Boolean = false,
  val serviceBeingStarted: Boolean? = null,
  val showLoading: Boolean = true,
) : com.thindie.rknzbl.engine.State

sealed interface ScreenCommand : Command {
  data object BackRequested : ScreenCommand

  data object RequestStoredProfiles : ScreenCommand

  data object StopService : ScreenCommand

  // Selection mode commands
  data class EnterMultiDeletionMode(val profile: ConnectionProfile) : ScreenCommand

  data class TogglePendingDelete(val profile: ConnectionProfile) : ScreenCommand

  data object ExitMultiDeletionMode : ScreenCommand

  data class Activate(val profile: ConnectionProfile) : ScreenCommand

  data class Delete(val profile: ConnectionProfile) : ScreenCommand

  data object BatchDelete : ScreenCommand
}

private fun FavoriteProfilesFlow.stateSink(screenScope: ScreenScope<ScreenState, ScreenCommand>) {
  screenScope.stateSink {
    sub(
      (appContext as Application).vpnRuntimeState,
    )
      .transition { s, vpnState ->
        val profiles = repository.read()
        val selected =
          when (vpnState) {
            WorkState.Idle -> null
            is WorkState.Error,
            WorkState.Running,
            -> {
              repository.activeProfile()
            }
          }
        val speedTestMessage =
          if (selected != null) {
            SpeedtestManager.testConnection(appContext, SettingsManager.getHttpPort())
          } else {
            null
          }
        speedTestMessage?.let {
          sendEvent(ServiceCommand.UiEvent.SnackText(it.message))
        }
        s.copy(
          serviceBeingStarted =
            when (vpnState) {
              is WorkState.Error -> null
              WorkState.Idle -> true
              WorkState.Running -> null
            }.takeIf { s.serviceBeingStarted != null },
          established =
            when (vpnState) {
              is WorkState.Error -> false
              WorkState.Idle -> false
              WorkState.Running -> true
            },
          selectedTestConnectionMessage = speedTestMessage,
          showLoading = false,
          selected = selected,
          profiles = profiles,
        )
      }
  }
}

private suspend fun FavoriteProfilesFlow.exec(
  c: ScreenCommand,
  s: ScreenState,
): ScreenState {
  return when (c) {
    ScreenCommand.BackRequested -> {
      finish(Unit)
      s
    }

    ScreenCommand.RequestStoredProfiles -> {
      withContext(Dispatchers.IO) {
        val parsed = repository.read()
        s.copy(
          profiles = parsed,
        )
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
            selectedProfiles = emptySet(),
            selectionMode = false,
          )
        } else {
          val selected = s.selectedProfiles
          repository.delete(c.profile)
          val profiles = repository.read()
          s.copy(
            selectedProfiles = selected,
            profiles = profiles,
          )
        }
      }
    }

    is ScreenCommand.Activate -> {
      withContext(Dispatchers.Default) {
        V2RayServiceManager.startVService(
          context = appContext,
          guid =
            KeyValueStorage.encodeServerConfig(
              guid = UUID.randomUUID().toString(),
              config = c.profile,
            ),
        )
        s.copy(
          selected = c.profile,
          serviceBeingStarted = true,
        )
      }
    }

    ScreenCommand.StopService -> {
      V2RayServiceManager.stopVService(appContext)
      s
    }

    is ScreenCommand.EnterMultiDeletionMode -> {
      s.copy(selectionMode = true)
    }

    is ScreenCommand.TogglePendingDelete -> {
      if (c.profile in s.selectedProfiles) {
        s.copy(
          selectedProfiles = s.selectedProfiles - c.profile,
        )
      } else {
        s.copy(
          selectedProfiles = s.selectedProfiles + c.profile,
        )
      }
    }

    is ScreenCommand.ExitMultiDeletionMode -> {
      s.copy(selectedProfiles = emptySet(), selectionMode = false)
    }

    ScreenCommand.BatchDelete -> {
      withContext(Dispatchers.IO) {
        var hasActiveProfile = false
        for (profile in s.selectedProfiles) {
          repository.delete(profile)
          if (s.selected?.subscriptionId == profile.subscriptionId) {
            hasActiveProfile = true
          }
        }

        if (hasActiveProfile) {
          V2RayServiceManager.stopVService(appContext)
        }
        val profiles = repository.read()
        s.copy(
          profiles = profiles,
          selectedProfiles = emptySet(),
          selectionMode = false,
          selected = if (hasActiveProfile) null else s.selected,
        )
      }
    }
  }
}

@Composable
private fun ScreenScope<ScreenState, ScreenCommand>.RouteScreen() {
  val screenState by state.collectAsState()
  AppScreen(
    primary =
      Action(
        resRef = R.drawable.ic_arrow_back_24,
        listener = {
          send(command = ScreenCommand.BackRequested)
        },
      ),
  ) {
    BackHandler { send(ScreenCommand.BackRequested) }
    val st by state.collectAsState()
    val height = LocalWindowInfo.current.containerSize.height.dp
    PullToRefreshBox(
      isRefreshing = false,
      modifier = Modifier.height(height),
      onRefresh = {
        send(ScreenCommand.RequestStoredProfiles)
      },
    ) {
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
                text = stringResource(R.string.source_stored),
                style = AppTheme.typography.headlineLarge,
                color = AppTheme.colors.contentPrimary,
              )
              val text =
                if (st.selectionMode) {
                  stringResource(R.string.source_stored_delete_active)
                } else {
                  stringResource(R.string.source_stored_delete_hint)
                }
              VSpacer(2.dp)
              Text(
                text = text,
                style = AppTheme.typography.labelMedium,
                color = AppTheme.colors.contentSecondary,
              )
              VSpacer(2.dp)
            }
          }
        }
        items(
          items = screenState.profiles,
        ) { item ->
          val isPendingDelete = item in screenState.selectedProfiles
          val profileRunning = screenState.selected?.subscriptionId == item.subscriptionId && screenState.established
          val isFailedSpeedTest = screenState.selectedTestConnectionMessage is SpeedtestManager.SpeedTestResult.Err
          SentenceRow(
            modifier =
              Modifier
                .border(
                  border =
                    BorderStroke(
                      width = 1.2.dp,
                      color =
                        if (isPendingDelete) {
                          AppTheme.colors.contentSecondary
                        } else if (profileRunning) {
                          if (isFailedSpeedTest) {
                            AppTheme.colors.errorPrimary
                          } else {
                            AppTheme.colors.accentPrimary
                          }
                        } else {
                          AppTheme.colors.backgroundSecondary
                        },
                    ),
                  shape = RoundedCornerShape(20.dp),
                )
                .fillMaxWidth(),
            painter =
              when {
                isPendingDelete -> {
                  painterResource(R.drawable.ic_close_16)
                }
                profileRunning -> {
                  if (isFailedSpeedTest) {
                    painterResource(R.drawable.ic_attention_24)
                  } else {
                    painterResource(R.drawable.ic_done_square_24)
                  }
                }
                else -> {
                  painterResource(R.drawable.ic_internet_24)
                }
              },
            title = item.remarks + " " + item.serverPort.orEmpty(),
            subtitle =
              when {
                st.serviceBeingStarted == true -> {
                  item.flow ?: item.server ?: item.serviceName.orEmpty()
                }
                profileRunning ->
                  if (st.selectedTestConnectionMessage == null) {
                    ""
                  } else {
                    st.selectedTestConnectionMessage?.message
                  }
                else -> {
                  item.flow ?: item.server ?: item.serviceName.orEmpty()
                }
              },
            loading = false,
            onClick = {
              if (screenState.selectionMode) {
                send(ScreenCommand.TogglePendingDelete(item))
              } else {
                send(ScreenCommand.Activate(item))
              }
            },
            onLongClick = {
              if (st.selectionMode) {
                send(ScreenCommand.ExitMultiDeletionMode)
              } else {
                send(ScreenCommand.EnterMultiDeletionMode(item))
              }
            },
          )
        }
        item {
          VSpacer(72.dp)
        }
      }
      val selectedCount = screenState.selectedProfiles.size
      Button(
        modifier =
          Modifier
            .align(Alignment.BottomCenter)
            .padding(16.dp),
        enabled = st.established || screenState.profiles.isEmpty() || selectedCount > 0,
        text =
          when {
            st.selectionMode -> stringResource(R.string.source_stored_selected_count, selectedCount)
            this@RouteScreen.processing.value is ScreenCommand.Activate -> ""
            screenState.profiles.isEmpty() -> stringResource(R.string.home_fetch_profiles)
            st.established -> stringResource(R.string.home_stop_service)
            else -> stringResource(R.string.home_pick_profile_first)
          },
        onClick = {
          when {
            selectedCount > 0 -> {
              sendEvent(
                ServiceCommand.UiEvent.Decision(
                  content = {
                    Aware(
                      painter = painterResource(R.drawable.ic_close_16),
                      title = stringResource(R.string.source_stored_delete),
                      subtitle = stringResource(R.string.source_stored_delete_subtitle),
                    )
                  },
                  primaryAction =
                    Action(
                      resRef = R.string.source_select_done,
                      listener = { send(ScreenCommand.BatchDelete) },
                    ),
                ),
              )
            }
            st.established -> send(ScreenCommand.StopService)
          }
        },
        loading = this@RouteScreen.processing.value is ScreenCommand.Activate,
      )
    }
  }
  if (screenState.serviceBeingStarted == true || screenState.showLoading) {
    Box(
      Modifier
        .fillMaxSize()
        .background(
          Color.Transparent.copy(alpha = 0.3f),
        )
        .clickable(onClick = {}, enabled = false),
    ) {
      CircularProgress(
        modifier =
          Modifier
            .align(Alignment.Center)
            .background(
              color = AppTheme.colors.backgroundSecondary,
              shape = RoundedCornerShape(20.dp),
            )
            .padding(16.dp),
      )
      AnimatedVisibility(
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(
              all = 16.dp,
            ),
        visible = screenState.serviceBeingStarted == true,
      ) {
        Row(
          modifier =
            Modifier
              .fillMaxWidth()
              .padding(
                all = 16.dp,
              )
              .surface(
                backgroundColor = AppTheme.colors.backgroundPrimary,
                shape = RoundedCornerShape(20.dp),
              )
              .height(56.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          HSpacer(16.dp)
          Text(
            stringResource(R.string.home_starting_vpn),
            style = AppTheme.typography.bodyMedium,
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
    modifier =
      modifier
        .background(
          color = AppTheme.colors.backgroundSecondary,
          shape = RoundedCornerShape(20.dp),
        )
        .clip(shape = RoundedCornerShape(20.dp))
        .padding(horizontal = 16.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(contentAlignment = Alignment.Center) {
      Icon(
        painter = painter,
        contentDescription = null,
        modifier =
          Modifier
            .background(
              color = AppTheme.colors.backgroundSecondary,
              shape = RoundedCornerShape(20.dp),
            )
            .padding(8.dp)
            .size(32.dp),
        tint = AppTheme.colors.errorPrimary,
      )
    }
    HSpacer(12.dp)
    if (subtitle != null) {
      Column {
        Text(
          text = title,
          style = AppTheme.typography.titleMedium,
          color = AppTheme.colors.contentPrimary,
        )
        VSpacer(2.dp)
        Text(
          text = subtitle,
          style = AppTheme.typography.bodyMedium,
          color = AppTheme.colors.contentSecondary,
        )
      }
    } else {
      Text(
        text = title,
        style = AppTheme.typography.titleMedium,
        color = AppTheme.colors.contentPrimary,
      )
    }
  }
}
