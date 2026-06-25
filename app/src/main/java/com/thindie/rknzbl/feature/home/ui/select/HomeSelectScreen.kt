package com.thindie.rknzbl.feature.home.ui.select

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.thindie.rknzbl.R
import com.thindie.rknzbl.engine.ScreenScope
import com.thindie.rknzbl.uikit.AppScreen
import com.thindie.rknzbl.uikit.AppTheme
import com.thindie.rknzbl.uikit.SentenceRow
import com.thindie.rknzbl.uikit.VSpacer
import com.thindie.rknzbl.uikit.WSpacer

@Composable
internal fun ScreenScope<ScreenState, ScreenCommand>.HomeSelectScreen() {
  AppScreen {
    BackHandler { send(ScreenCommand.Back) }
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(16.dp),
    ) {
      Row(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = stringResource(R.string.app_name),
          style = AppTheme.typography.headlineLarge,
          color = AppTheme.colors.contentPrimary,
        )
      }
      Text(
        text = stringResource(R.string.home_select_tagline),
        style = AppTheme.typography.labelMedium,
        color = AppTheme.colors.contentSecondary,
      )
      VSpacer(24.dp)
      SentenceRow(
        modifier = Modifier.fillMaxWidth(),
        title = stringResource(R.string.home_select_new_profiles_title),
        subtitle = stringResource(R.string.home_select_new_profiles_subtitle),
        painter = painterResource(R.drawable.ic_internet_24),
        onClick = { send(ScreenCommand.New) },
        loading = false,
      )
      VSpacer(16.dp)
      SentenceRow(
        modifier = Modifier.fillMaxWidth(),
        title = stringResource(R.string.home_select_stored_title),
        subtitle = stringResource(R.string.home_select_stored_subtitle),
        painter = painterResource(R.drawable.ic_home_24),
        onClick = { send(ScreenCommand.Home) },
        loading = false,
      )
      VSpacer(16.dp)
      SentenceRow(
        modifier = Modifier.fillMaxWidth(),
        title = stringResource(R.string.home_select_vpn_modes_title),
        subtitle = stringResource(R.string.home_select_vpn_modes_subtitle),
        painter = painterResource(R.drawable.ic_filter_24),
        onClick = { send(ScreenCommand.PerAppProxy) },
        loading = false,
      )
      VSpacer(16.dp)
      SentenceRow(
        modifier = Modifier.fillMaxWidth(),
        title = stringResource(R.string.home_select_settings_title),
        subtitle = stringResource(R.string.home_select_settings_subtitle),
        painter = painterResource(R.drawable.ic_settings_24),
        onClick = { send(ScreenCommand.Settings) },
        loading = false,
      )
      val profile = state.collectAsState().value.autoSaved
      if (profile != null) {
        WSpacer()
        Text(
          text = stringResource(R.string.home_autosaved_profile),
          style = AppTheme.typography.bodyMedium,
          color = AppTheme.colors.contentPrimary,
        )
        VSpacer(2.dp)
        SentenceRow(
          modifier =
            Modifier
              .border(
                border = BorderStroke(1.2.dp, AppTheme.colors.backgroundSecondary),
                shape = RoundedCornerShape(20.dp),
              )
              .fillMaxWidth(),
          painter = painterResource(R.drawable.ic_folder_24),
          title = profile.remarks + profile.serverPort.orEmpty(),
          subtitle = profile.flow ?: profile.server ?: profile.serviceName ?: "",
          loading = false,
          onClick = { send(ScreenCommand.DismissAutoSaved) },
          onLongClick = null,
        )
      }
    }
  }
}
