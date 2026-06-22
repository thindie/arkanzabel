package com.thindie.rknzbl.feature.managegate.storedgates.profiles

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.thindie.rknzbl.R
import com.thindie.rknzbl.engine.ScreenScope
import com.thindie.rknzbl.engine.ServiceCommand
import com.thindie.rknzbl.uikit.Action
import com.thindie.rknzbl.uikit.AppScreen
import com.thindie.rknzbl.uikit.AppTheme
import com.thindie.rknzbl.uikit.Button
import com.thindie.rknzbl.uikit.HSpacer
import com.thindie.rknzbl.uikit.SentenceRow
import com.thindie.rknzbl.uikit.VSpacer
import com.v2ray.ang.runtime.SpeedtestManager
import kotlinx.coroutines.delay

@Composable
internal fun ScreenScope<ScreenState, ScreenCommand>.ProfilesScreen() {
  val screenState by state.collectAsState()
  val established =
    screenState.selectedTestConnectionMessage is SpeedtestManager.SpeedTestResult.Ok
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
          val profileRunning = screenState.selected?.subscriptionId == item.subscriptionId && established
          val isFailedSpeedTest =
            screenState.selectedTestConnectionMessage is SpeedtestManager.SpeedTestResult.Err
          SentenceRow(
            modifier =
              Modifier
                .then(
                  if (screenState.selected == item) {
                    if (st.selectedTestConnectionMessage == null) {
                      var progress by remember { mutableStateOf(0f) }
                      var moveRight by remember { mutableStateOf(true) }
                      LaunchedEffect(Unit) {
                        while (true) {
                          if (moveRight) {
                            if (progress > 1f) {
                              moveRight = false
                              progress -= 0.1f
                            } else {
                              progress += 0.1f
                            }
                          } else {
                            if (progress < -1f) {
                              moveRight = true
                              progress += 0.1f
                            } else {
                              progress -= 0.1f
                            }
                          }
                          delay(50)
                        }
                      }

                      Modifier
                        .border(
                          brush =
                            Brush.linearGradient(
                              colors =
                                listOf(
                                  AppTheme.colors.contentPrimary,
                                  AppTheme.colors.contentSecondary,
                                  AppTheme.colors.backgroundSecondary,
                                  AppTheme.colors.contentSecondary,
                                  AppTheme.colors.backgroundPrimary,
                                ),
                              start = Offset(progress * 500f, 0f),
                              end = Offset((progress + 1f) * 500f, 200f),
                            ),
                          shape = RoundedCornerShape(20.dp),
                          width = 1.2.dp,
                        )
                    } else {
                      Modifier
                        .border(
                          border =
                            BorderStroke(
                              width = 1.2.dp,
                              color =
                                when (st.selectedTestConnectionMessage) {
                                  is SpeedtestManager.SpeedTestResult.Err -> AppTheme.colors.errorPrimary
                                  is SpeedtestManager.SpeedTestResult.Ok -> AppTheme.colors.accentPrimary
                                  null -> AppTheme.colors.backgroundSecondary
                                },
                            ),
                          shape = RoundedCornerShape(20.dp),
                        )
                    }
                  } else {
                    Modifier
                      .border(
                        border =
                          BorderStroke(
                            width = 1.2.dp,
                            color =
                              if (screenState.selected == item && established) {
                                AppTheme.colors.accentPrimary
                              } else {
                                AppTheme.colors.backgroundSecondary
                              },
                          ),
                        shape = RoundedCornerShape(20.dp),
                      )
                  },
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
            loading = st.selectedTestConnectionMessage == null && st.selected == item,
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
        enabled = established || screenState.profiles.isEmpty() || selectedCount > 0,
        text =
          when {
            st.selectionMode -> stringResource(R.string.source_stored_selected_count, selectedCount)
            this@ProfilesScreen.processing.value is ScreenCommand.Activate -> ""
            screenState.profiles.isEmpty() -> stringResource(R.string.home_fetch_profiles)
            established -> stringResource(R.string.home_stop_service)
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

            established -> send(ScreenCommand.StopService)
          }
        },
        loading = this@ProfilesScreen.processing.value is ScreenCommand.Activate,
      )
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
