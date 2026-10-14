package com.thindie.rknzbl.appfeatures.profiles.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.thindie.engine.core.ScreenScope
import com.thindie.engine.uikit.AppScreen
import com.thindie.engine.uikit.AppTheme
import com.thindie.engine.uikit.Button
import com.thindie.engine.uikit.CircularProgress
import com.thindie.engine.uikit.ProfileBorderState
import com.thindie.engine.uikit.SentenceRow
import com.thindie.engine.uikit.SentenceRowIcon
import com.thindie.engine.uikit.TabItem
import com.thindie.engine.uikit.TabRow
import com.thindie.engine.uikit.VSpacer
import com.thindie.engine.uikit.profileBorder
import com.thindie.engine.uikit.surface
import com.thindie.rknzbl.R
import com.thindie.rknzbl.appfeatures.profiles.component.EmptyProfilesContent
import com.v2ray.ang.dto.ConnectionProfile

@Composable
fun ProfilesScreenContent(scope: ScreenScope<ScreenState, ScreenCommand>) {
  val st by scope.state.collectAsState()
  AppScreen(scope) {
    Box(modifier = Modifier.fillMaxSize()) {
      PullToRefreshBox(
        modifier = Modifier.fillMaxSize(),
        isRefreshing = scope.processing.value is ScreenCommand.RefreshProfiles,
        onRefresh = { scope.send(ScreenCommand.RefreshProfiles) },
      ) {
        Column(modifier = Modifier.fillMaxHeight().padding(16.dp)) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = stringResource(R.string.profiles_title),
              style = AppTheme.typography.headlineLarge,
              color = AppTheme.colors.contentPrimary,
            )
            AnimatedVisibility(
              visible = st.selectedTab == 1,
            ) {
              VSpacer(8.dp)
              Text(
                text =
                  if (st.isLocalSave) {
                    stringResource(R.string.source_stored_local)
                  } else {
                    stringResource(R.string.source_stored_remote)
                  },
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.contentSecondary,
              )
            }
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
              val profilesToShow = (if (it == 0) st.profiles else st.savedProfiles)
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
                      leading = {
                        SentenceRowIcon(painter = painterResource(R.drawable.ic_internet_24))
                      },
                      content = {
                        Text(
                          text = profile.remarks,
                          style = AppTheme.typography.titleMedium,
                          color = AppTheme.colors.contentPrimary,
                        )
                        VSpacer(2.dp)
                        Text(
                          text = profileSubtitle(profile),
                          style = AppTheme.typography.bodyMedium,
                          color = AppTheme.colors.contentSecondary,
                        )
                      },
                      loading = false,
                      enabled = profile.delay != -1L,
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

      if (st.selectedTab == 0 && st.profiles.isNotEmpty()) {
        ProfilesFab(
          modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
          onClick = { scope.send(ScreenCommand.PingAllProfiles) },
          loading = st.pingInProgress,
        )
      }
    }
  }
}

@Composable
private fun profileSubtitle(item: ConnectionProfile): String {
  val unreachable = stringResource(R.string.home_profile_unreachable)
  val ping = item.delay
  return when {
    ping == null -> item.flow ?: item.server ?: item.serviceName ?: ""
    ping < 0 -> unreachable
    else -> stringResource(R.string.home_profile_ping_ms, ping.toString())
  }
}

@Composable
private fun ProfilesFab(
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
  loading: Boolean = false,
) {
  val backgroundColor by animateColorAsState(
    targetValue = if (loading) AppTheme.colors.backgroundSecondary else AppTheme.colors.accentPrimary,
    label = "fab-bg",
  )
  Box(
    modifier =
      modifier
        .size(52.dp)
        .surface(
          shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
          backgroundColor = backgroundColor,
          enabled = true,
          onClick = onClick,
        ),
    contentAlignment = Alignment.Center,
  ) {
    if (loading) {
      CircularProgress(modifier = Modifier.size(24.dp))
    } else {
      Icon(
        painter = painterResource(R.drawable.ic_clock_24),
        contentDescription = stringResource(R.string.profiles_ping_all),
        tint = AppTheme.colors.onAccentPrimary,
      )
    }
  }
}

@Preview
@Composable
private fun ProfilesFabPreview() {
  AppTheme {
    Box(modifier = Modifier.fillMaxSize()) {
      ProfilesFab(
        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        onClick = {},
        loading = false,
      )
    }
  }
}

@Preview
@Composable
private fun ProfilesFabLoadingPreview() {
  AppTheme {
    Box(modifier = Modifier.fillMaxSize()) {
      ProfilesFab(
        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        onClick = {},
        loading = true,
      )
    }
  }
}
