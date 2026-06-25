package com.thindie.rknzbl.feature.home.ui.newprofiles

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.thindie.rknzbl.uikit.ProfileBorderState
import com.thindie.rknzbl.uikit.SentenceRow
import com.thindie.rknzbl.uikit.VSpacer
import com.thindie.rknzbl.uikit.profileBorder
import com.v2ray.ang.runtime.SpeedtestManager

@Composable
fun ScreenScope<ScreenState, ScreenCommand>.NewProfiles() {
  val st by state.collectAsState()
  val established = st.selectedTestConnectionMessage is SpeedtestManager.SpeedTestResult.Ok
  AppScreen(
    primary =
      Action(
        resRef = R.drawable.ic_arrow_back_24,
        listener = { send(ScreenCommand.Back) },
      ),
  ) {
    BackHandler { send(ScreenCommand.Back) }
    val height = LocalWindowInfo.current.containerSize.height.dp
    PullToRefreshBox(
      isRefreshing = false,
      modifier = Modifier.height(height),
      onRefresh = { send(ScreenCommand.Refresh) },
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
            Text(
              text = stringResource(R.string.home_downloaded_profiles_header),
              style = AppTheme.typography.headlineLarge,
              color = AppTheme.colors.contentPrimary,
            )
          }
        }
        item {
          SentenceRow(
            painter = painterResource(R.drawable.ic_chevron_right_24),
            title =
              if (st.sourceName.isBlank()) {
                stringResource(R.string.home_choose_source)
              } else {
                stringResource(R.string.home_source_selected_prefix)
              },
            subtitle = st.sourceName,
            onClick = { send(ScreenCommand.Choose) },
            loading = false,
          )
        }
        item {
          SentenceRow(
            painter = painterResource(R.drawable.ic_information_24),
            title = stringResource(R.string.per_app_proxy_row_title),
            subtitle = stringResource(R.string.per_app_proxy_row_subtitle),
            onClick = { send(ScreenCommand.OpenPerAppProxy) },
            loading = false,
          )
        }
        items(items = st.links) { item ->
          val borderState =
            when {
              st.selected != item -> ProfileBorderState.Inactive
              st.selectedTestConnectionMessage == null -> ProfileBorderState.Testing
              st.selectedTestConnectionMessage is SpeedtestManager.SpeedTestResult.Ok ->
                ProfileBorderState.Connected
              else -> ProfileBorderState.Failed
            }
          SentenceRow(
            modifier =
              Modifier
                .profileBorder(borderState)
                .fillMaxWidth(),
            painter = painterResource(R.drawable.ic_internet_24),
            title = item.remarks + item.serverPort.orEmpty(),
            subtitle = item.flow ?: item.server ?: item.serviceName ?: "",
            loading = st.selectedTestConnectionMessage == null && st.selected == item,
            onClick = { send(ScreenCommand.Select(item)) },
            onLongClick =
              if (st.selected == item) {
                {
                  sendEvent(
                    ServiceCommand.UiEvent.Decision(
                      content = {
                        Text(
                          text = stringResource(R.string.source_stored_add),
                          style = AppTheme.typography.bodyMedium,
                        )
                      },
                      primaryAction =
                        Action(
                          resRef = R.string.source_select_done,
                          listener = { send(ScreenCommand.Save(item)) },
                        ),
                    ),
                  )
                }
              } else {
                null
              },
          )
        }
        item {
          VSpacer(72.dp)
        }
      }
      Button(
        modifier =
          Modifier
            .align(Alignment.BottomCenter)
            .padding(16.dp),
        enabled = established || st.links.isEmpty(),
        text =
          when {
            this@NewProfiles.processing.value == ScreenCommand.Start -> ""
            st.links.isEmpty() -> stringResource(R.string.home_fetch_profiles)
            established -> stringResource(R.string.home_stop_service)
            else -> stringResource(R.string.home_pick_profile_first)
          },
        onClick = {
          if (established) {
            send(ScreenCommand.Stop)
          } else {
            send(ScreenCommand.Start)
          }
        },
      )
    }
  }
}
