package com.thindie.rknzbl.appfeatures.home.ui

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
import com.thindie.engine.core.ScreenScope
import com.thindie.engine.uikit.AppScreen
import com.thindie.engine.uikit.AppTheme
import com.thindie.engine.uikit.SentenceRow
import com.thindie.engine.uikit.VSpacer
import com.thindie.engine.uikit.WSpacer
import com.thindie.rknzbl.R

/**
 * Home tab screen for the new bottom-nav design. Mirrors the legacy `select` hub: entry points to
 * stored profiles, new profile download and per-app proxy settings — minus the Settings row (the
 * bottom bar handles it).
 */
@Composable
internal fun HomeScreenContent(scope: ScreenScope<ScreenState, ScreenCommand>) {
  AppScreen(scope) {
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
        onClick = { scope.send(ScreenCommand.New) },
        loading = false,
      )
      VSpacer(16.dp)
      SentenceRow(
        modifier = Modifier.fillMaxWidth(),
        title = stringResource(R.string.home_select_stored_title),
        subtitle = stringResource(R.string.home_select_stored_subtitle),
        painter = painterResource(R.drawable.ic_home_24),
        onClick = { scope.send(ScreenCommand.Home) },
        loading = false,
      )
      VSpacer(16.dp)
      SentenceRow(
        modifier = Modifier.fillMaxWidth(),
        title = stringResource(R.string.home_select_vpn_modes_title),
        subtitle = stringResource(R.string.home_select_vpn_modes_subtitle),
        painter = painterResource(R.drawable.ic_filter_24),
        onClick = { scope.send(ScreenCommand.PerAppProxy) },
        loading = false,
      )
      val profile = scope.state.collectAsState().value.autoSaved
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
          onClick = { scope.send(ScreenCommand.DismissAutoSaved) },
          onLongClick = null,
        )
      }
    }
  }
}
