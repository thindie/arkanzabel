package com.thindie.rknzbl.appfeatures.help

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.thindie.engine.core.ScreenScope
import com.thindie.engine.uikit.Action
import com.thindie.engine.uikit.AppScreen
import com.thindie.engine.uikit.AppTheme
import com.thindie.engine.uikit.VSpacer
import com.thindie.rknzbl.R
import com.thindie.rknzbl.appfeatures.settings.ui.FaqPortalRow

@Composable
internal fun HelpScreenContent(scope: ScreenScope<HelpState, HelpCommand>) {
  AppScreen(
    screenScope = scope,
    title = null,
    primary =
      Action(
        listener = { scope.send(HelpCommand.Back) },
        resRef = R.drawable.ic_arrow_back_24,
      ),
  ) {
    BackHandler { scope.send(HelpCommand.Back) }
    Text(
      modifier = Modifier.padding(horizontal = 16.dp),
      text = stringResource(R.string.settings_section_help),
      style = AppTheme.typography.headlineLarge,
      color = AppTheme.colors.contentPrimary,
    )
    VSpacer(24.dp)
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
      item {
        HelpRows(
          onFaq = { scope.send(HelpCommand.OpenFaq) },
          onLicence = { scope.send(HelpCommand.OpenLicenses) },
          onSetupHelp = { scope.send(HelpCommand.OpenVpnSetup) },
        )
      }
    }
  }
}

@Composable
private fun HelpRows(
  onFaq: () -> Unit,
  onLicence: () -> Unit,
  onSetupHelp: () -> Unit,
) {
  VSpacer(16.dp)
  FaqPortalRow(
    label = stringResource(R.string.faq_row_title),
    subtitle = stringResource(R.string.faq_row_subtitle),
    onClick = onFaq,
  )
  VSpacer(8.dp)
  FaqPortalRow(
    label = stringResource(R.string.licenses_row_title),
    subtitle = stringResource(R.string.licenses_row_subtitle),
    onClick = onLicence,
  )
  VSpacer(8.dp)
  FaqPortalRow(
    label = stringResource(R.string.vpnsetup_row_title),
    subtitle = stringResource(R.string.vpnsetup_row_subtitle),
    onClick = onSetupHelp,
  )
  VSpacer(16.dp)
}

@Preview(showBackground = true)
@Composable
private fun HelpScreenContentPreview() {
  AppTheme {
    HelpRows(
      {},
      onLicence = {},
      onSetupHelp = {},
    )
  }
}
