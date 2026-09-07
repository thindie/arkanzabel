package com.thindie.rknzbl.appfeatures.profiles.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.thindie.engine.core.ScreenScope
import com.thindie.engine.uikit.AppScreen
import com.thindie.engine.uikit.AppTheme
import com.thindie.engine.uikit.ProfileBorderState
import com.thindie.engine.uikit.SentenceRow
import com.thindie.engine.uikit.TabItem
import com.thindie.engine.uikit.TabRow
import com.thindie.engine.uikit.VSpacer
import com.thindie.engine.uikit.profileBorder
import com.thindie.rknzbl.R
import com.v2ray.ang.dto.ConnectionProfile
import com.v2ray.ang.enums.NetworkType

@Composable
fun ProfilesScreenContent(scope: ScreenScope<ScreenState, ScreenCommand>) {
  val st by scope.state.collectAsState()
  AppScreen(scope) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = stringResource(R.string.profiles_title),
        style = AppTheme.typography.headlineLarge,
        color = AppTheme.colors.contentPrimary,
      )
      VSpacer(24.dp)
      TabRow(
        items =
          listOf(
            TabItem(stringResource(R.string.profiles_tab_main)),
            TabItem(stringResource(R.string.profiles_tab_saved)),
          ),
        selected = st.selectedTab,
        onTabSelected = { scope.send(ScreenCommand.SelectTab(it)) },
      ) {
        val profilesToShow = if (it == 0) st.profiles else st.savedProfiles

        VSpacer(16.dp)

        // Refresh profiles on saved tab
        if (it == 1) {
          Row(
            modifier = Modifier.padding(bottom = 8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
          ) {
            Button(
              onClick = { scope.send(ScreenCommand.RefreshProfiles) },
            ) {
              Text(stringResource(R.string.profiles_refresh))
            }
          }
        }

        if (profilesToShow.isEmpty()) {
          Text(
            text = stringResource(R.string.profiles_empty),
            modifier = Modifier.fillMaxWidth(),
          )
        } else {
          LazyColumn {
            // No custom keys: subscriptionId is not unique across parsed profiles,
            // which would crash LazyColumn with duplicate keys.
            items(profilesToShow) { profile ->
              val borderState =
                when {
                  st.connectedProfile == profile -> ProfileBorderState.Connected
                  else -> ProfileBorderState.Inactive
                }

              SentenceRow(
                modifier = Modifier.profileBorder(borderState).fillMaxWidth(),
                painter = painterResource(R.drawable.ic_internet_24),
                title = profile.remarks,
                subtitle = profileSubtitle(profile, st.pingResults[profile.subscriptionId]),
                loading = false,
                onClick = { scope.send(ScreenCommand.ConnectProfile(profile)) },
              )

              VSpacer(8.dp)
            }
          }
        }
      }
    }
  }
}

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
  return transportLabel(item.network).let { label ->
    if (label.isEmpty()) base else "$base · $label"
  }
}

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
