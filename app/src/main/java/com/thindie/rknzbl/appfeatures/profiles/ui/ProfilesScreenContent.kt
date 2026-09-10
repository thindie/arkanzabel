package com.thindie.rknzbl.appfeatures.profiles.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.thindie.engine.core.ScreenScope
import com.thindie.engine.uikit.AppScreen
import com.thindie.engine.uikit.AppTheme
import com.thindie.engine.uikit.Button
import com.thindie.engine.uikit.CircularProgress
import com.thindie.engine.uikit.ProfileBorderState
import com.thindie.engine.uikit.SentenceRow
import com.thindie.engine.uikit.TabItem
import com.thindie.engine.uikit.TabRow
import com.thindie.engine.uikit.VSpacer
import com.thindie.engine.uikit.profileBorder
import com.thindie.rknzbl.R
import com.thindie.rknzbl.appfeatures.profiles.component.EmptyProfilesContent
import com.v2ray.ang.dto.ConnectionProfile
import com.v2ray.ang.enums.NetworkType

@Composable
fun ProfilesScreenContent(scope: ScreenScope<ScreenState, ScreenCommand>) {
  val st by scope.state.collectAsState()
  AppScreen(scope) {
    Column(modifier = Modifier.fillMaxHeight().padding(16.dp)) {
      Column(modifier = Modifier.weight(1f)) {
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

          val loading = it == 0 && st.profilesLoading

          if (loading) {
            Box(
              modifier = Modifier.fillMaxSize(),
              contentAlignment = Alignment.Center,
            ) {
              CircularProgress()
            }
          } else if (profilesToShow.isEmpty()) {
            EmptyProfilesContent(
              icon = if (it == 1) R.drawable.ic_folder_24 else R.drawable.ic_globus_24,
              title =
                if (it == 1) {
                  stringResource(R.string.profiles_saved_empty)
                } else {
                  stringResource(R.string.profiles_empty)
                },
              subtitle =
                if (it == 1) {
                  null
                } else {
                  stringResource(R.string.home_select_new_profiles_subtitle)
                },
            )
          } else {
            VSpacer(16.dp)
            LazyColumn(modifier = Modifier.fillMaxHeight()) {
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

      if (st.selectedTab == 1 && st.savedProfiles.isNotEmpty()) {
        VSpacer(24.dp)
        Button(
          text = stringResource(R.string.profiles_open_delete),
          onClick = { scope.send(ScreenCommand.OpenDeleteSavedProfiles) },
        )
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
