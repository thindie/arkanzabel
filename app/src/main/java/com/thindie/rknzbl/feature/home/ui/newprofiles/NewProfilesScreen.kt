package com.thindie.rknzbl.feature.home.ui.newprofiles

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.CircularProgressIndicator
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
import com.thindie.engine.core.ScreenScope
import com.thindie.engine.core.ServiceCommand
import com.thindie.engine.core.WorkState
import com.thindie.engine.uikit.Action
import com.thindie.engine.uikit.AppScreen
import com.thindie.engine.uikit.AppTheme
import com.thindie.engine.uikit.Button
import com.thindie.engine.uikit.ProfileBorderState
import com.thindie.engine.uikit.SentenceRow
import com.thindie.engine.uikit.VSpacer
import com.thindie.engine.uikit.profileBorder
import com.thindie.rknzbl.R
import com.v2ray.ang.dto.ConnectionProfile
import com.v2ray.ang.enums.NetworkType
import com.v2ray.ang.enums.Protocol
import com.v2ray.ang.runtime.SpeedtestManager
import java.util.Comparator

@Composable
fun NewProfiles(scope: ScreenScope<ScreenState, ScreenCommand>) {
  val st by scope.state.collectAsState()
  val established = st.selectedTestConnectionMessage is SpeedtestManager.SpeedTestResult.Ok
  AppScreen(
    screenScope = scope,
    primary =
      Action(
        resRef = R.drawable.ic_arrow_back_24,
        listener = { scope.send(ScreenCommand.Back) },
      ),
  ) {
    BackHandler { scope.send(ScreenCommand.Back) }
    val height = LocalWindowInfo.current.containerSize.height.dp
    PullToRefreshBox(
      isRefreshing = scope.processing.value == ScreenCommand.Refresh,
      modifier = Modifier.height(height),
      onRefresh = { scope.send(ScreenCommand.Refresh) },
    ) {
      LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        stickyHeader {
          Column(
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
            FilterToggle(
              selected = st.filter,
              enabled = !st.pingResults.isNullOrEmpty(),
              loading = st.pingState is WorkState.Running,
              onAll = { scope.send(ScreenCommand.Filter(FilterMode.All)) },
              onAvailable = { scope.send(ScreenCommand.Filter(FilterMode.Available)) },
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
            onClick = { scope.send(ScreenCommand.Choose) },
            loading = false,
          )
        }
        item {
          SentenceRow(
            painter = painterResource(R.drawable.ic_information_24),
            title = stringResource(R.string.per_app_proxy_row_title),
            subtitle = stringResource(R.string.per_app_proxy_row_subtitle),
            onClick = { scope.send(ScreenCommand.OpenPerAppProxy) },
            loading = false,
          )
        }
        val visible =
          when (st.filter) {
            FilterMode.All -> st.links
            FilterMode.Available -> st.pingResults?.keys?.toList().orEmpty()
          }

        transportSections(visible)
          .forEach { section ->
            stickyHeader {
              Text(
                text = section.key.protocolScheme,
                style = AppTheme.typography.titleSmall,
                color = AppTheme.colors.contentSecondary,
                modifier =
                  Modifier
                    .fillMaxWidth()
                    .background(AppTheme.colors.backgroundPrimary)
                    .padding(vertical = 8.dp, horizontal = 4.dp),
              )
            }
            items(items = section.value) { item ->
              val borderState =
                when {
                  st.selected != item -> ProfileBorderState.Inactive
                  st.selectedTestConnectionMessage == null -> ProfileBorderState.Testing
                  st.selectedTestConnectionMessage is SpeedtestManager.SpeedTestResult.Ok ->
                    ProfileBorderState.Connected
                  else -> ProfileBorderState.Failed
                }
              val ping = st.pingResults?.get(item)
              SentenceRow(
                modifier =
                  Modifier
                    .profileBorder(borderState)
                    .fillMaxWidth(),
                painter = painterResource(R.drawable.ic_internet_24),
                title = item.remarks + item.serverPort.orEmpty(),
                subtitle = profileSubtitle(item, ping),
                loading = st.selectedTestConnectionMessage == null && st.selected == item,
                onClick = { scope.send(ScreenCommand.Select(item)) },
                onLongClick =
                  if (st.selected == item) {
                    {
                      scope.sendEvent(
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
                              listener = { scope.send(ScreenCommand.Save(item)) },
                            ),
                        ),
                      )
                    }
                  } else {
                    null
                  },
              )
            }
          }
        if (visible.isEmpty()) {
          item {
            Text(
              text = stringResource(R.string.home_filter_no_available),
              style = AppTheme.typography.bodyMedium,
              color = AppTheme.colors.contentSecondary,
            )
          }
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
            scope.processing.value == ScreenCommand.Start -> ""
            st.links.isEmpty() -> stringResource(R.string.home_fetch_profiles)
            established -> stringResource(R.string.home_stop_service)
            else -> stringResource(R.string.home_pick_profile_first)
          },
        onClick = {
          if (established) {
            scope.send(ScreenCommand.Stop)
          } else {
            scope.send(ScreenCommand.Start)
          }
        },
      )
    }
  }
}

/**
 * Orders profiles so that those with a known latency come first, sorted ascending by delay
 * (faster first, unreachable last). Profiles without a ping result keep their relative order.
 */
private fun pingOrder(pingResults: Map<ConnectionProfile, Long>): Comparator<ConnectionProfile> =
  compareBy<ConnectionProfile> { profile ->
    when (val delay = pingResults[profile]) {
      null -> Long.MAX_VALUE
      else -> delay
    }
  }

/**
 * Renders the ping result for a profile: a "checking" label while it is being measured in the
 * background, the delay in milliseconds once measured, or a fallback label when the profile is
 * unreachable or has not been measured yet.
 */
@Composable
private fun profileSubtitle(
  item: ConnectionProfile,
  ping: Long?,
): String {
  val base =
    when {
      ping == null -> item.flow ?: item.server ?: item.serviceName ?: ""
      ping < 0 -> stringResource(R.string.home_profile_unreachable)
      else -> stringResource(R.string.home_profile_ping_ms, ping)
    }
  return transportLabel(item.network).let { label -> if (label.isEmpty()) base else "$base · $label" }
}

/**
 * Human-readable transport for [ConnectionProfile.network] so the user can see which transport a
 * profile uses. TCP is omitted because it is the detectable baseline; non-TCP transports are the
 * meaningful stealth signal, and skipping TCP keeps cards tidy for the common case.
 */
private fun transportLabel(network: NetworkType): String {
  return when (network) {
    NetworkType.WS -> "WebSocket"
    NetworkType.HTTP_UPGRADE -> "HTTP Upgrade"
    NetworkType.XHTTP -> "XHTTP"
    NetworkType.H2 -> "HTTP/2"
    NetworkType.GRPC -> "gRPC"
    NetworkType.KCP -> "KCP"
    NetworkType.HTTP -> "HTTP"
    else -> network.type.uppercase()
  }
}

private fun transportSections(profiles: List<ConnectionProfile>): Map<Protocol, List<ConnectionProfile>> {
  return profiles.groupBy { it.protocol }
}

@Composable
private fun FilterToggle(
  selected: FilterMode,
  enabled: Boolean,
  loading: Boolean,
  onAll: () -> Unit,
  onAvailable: () -> Unit,
) {
  val options = listOf(FilterMode.All, FilterMode.Available)
  val selectedIndex = options.indexOf(selected)
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .padding(vertical = 12.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    options.forEachIndexed { index, option ->
      val isSelected = index == selectedIndex
      val onOption = if (option == FilterMode.All) onAll else onAvailable
      val disabled = option == FilterMode.Available && !enabled
      val label =
        if (option == FilterMode.All) {
          stringResource(R.string.home_filter_all)
        } else {
          stringResource(R.string.home_filter_available)
        }
      Box(
        modifier =
          Modifier
            .weight(1f)
            .background(
              color =
                if (isSelected) {
                  AppTheme.colors.accentPrimary
                } else {
                  AppTheme.colors.backgroundSecondary
                },
              shape = RoundedCornerShape(8.dp),
            )
            .clickable(onClick = onOption, enabled = !disabled)
            .padding(vertical = 8.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center,
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          if (option == FilterMode.Available && loading) {
            CircularProgressIndicator(
              modifier = Modifier.size(14.dp),
              color = if (isSelected) AppTheme.colors.onAccentPrimary else AppTheme.colors.contentSecondary,
              strokeWidth = 2.dp,
            )
          }
          Text(
            text = label,
            style =
              if (isSelected) {
                AppTheme.typography.titleSmall
              } else {
                AppTheme.typography.bodyMedium
              },
            color =
              when {
                disabled -> AppTheme.colors.contentTertiary
                isSelected -> AppTheme.colors.onAccentPrimary
                else -> AppTheme.colors.contentSecondary
              },
          )
        }
      }
    }
  }
}
