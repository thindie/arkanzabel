package com.thindie.rknzbl.appfeatures.help.vpnsetup

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.thindie.engine.core.ScreenScope
import com.thindie.engine.uikit.Action
import com.thindie.engine.uikit.AppScreen
import com.thindie.engine.uikit.AppTheme
import com.thindie.engine.uikit.HSpacer
import com.thindie.engine.uikit.HelpParagraph
import com.thindie.engine.uikit.HelpSectionHeading
import com.thindie.engine.uikit.TopAppBar
import com.thindie.engine.uikit.VSpacer
import com.thindie.rknzbl.R

@Composable
internal fun VpnSetupScreenContent(scope: ScreenScope<VpnSetupState, VpnSetupCommand>) {
  AppScreen(scope) {
    BackHandler { scope.send(VpnSetupCommand.Back) }

    TopAppBar(
      primary =
        Action(
          listener = { scope.send(VpnSetupCommand.Back) },
          resRef = R.drawable.ic_arrow_back_24,
        ),
    )

    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(16.dp),
    ) {
      Text(
        text = stringResource(R.string.vpnsetup_screen_title),
        style = AppTheme.typography.headlineLarge,
        color = AppTheme.colors.contentPrimary,
      )
      Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        VSpacer(24.dp)
        Divider()
        VSpacer(16.dp)
        HelpSectionHeading(stringResource(R.string.vpnsetup_steps_title))
        VSpacer(8.dp)
        HelpParagraph(stringResource(R.string.vpnsetup_intro))

        VSpacer(16.dp)
        for (index in 0 until STEPS.size) {
          VpnStep(number = index + 1, text = stringResource(STEPS[index].stringRes))
          VSpacer(2.dp)
        }

        VSpacer(24.dp)
        Divider()
        VSpacer(16.dp)
        HelpSectionHeading(stringResource(R.string.vpnsetup_permission_title))
        VSpacer(8.dp)
        HelpParagraph(stringResource(R.string.vpnsetup_permission_body))

        VSpacer(24.dp)
        Divider()
        VSpacer(16.dp)
        HelpSectionHeading(stringResource(R.string.vpnsetup_keep_title))
        VSpacer(8.dp)
        HelpParagraph(stringResource(R.string.vpnsetup_keep_body))

        VSpacer(16.dp)
        for (index in 0 until DEFAULT_STEPS.size) {
          VpnStep(number = index + 1, text = stringResource(DEFAULT_STEPS[index].stringRes))
          VSpacer(2.dp)
        }

        VSpacer(24.dp)
        Divider()
        VSpacer(16.dp)
        HelpSectionHeading(stringResource(R.string.vpnsetup_battery_title))
        VSpacer(8.dp)
        HelpParagraph(stringResource(R.string.vpnsetup_battery_body))

        VSpacer(16.dp)
        for (index in 0 until BATTERY_STEPS.size) {
          VpnStep(number = index + 1, text = stringResource(BATTERY_STEPS[index].stringRes))
          VSpacer(2.dp)
        }

        VSpacer(32.dp)
      }
    }
  }
}

@Composable
private fun VpnStep(
  number: Int,
  text: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = "$number.",
      style = AppTheme.typography.titleLarge,
      color = AppTheme.colors.accentPrimary,
    )
    HSpacer(8.dp)
    Text(
      text = text,
      modifier = Modifier.weight(1f),
      style = AppTheme.typography.bodyMedium,
      color = AppTheme.colors.contentPrimary,
    )
  }
}

@Preview(showBackground = true)
@Composable
private fun VpnSetupScreenContentPreview() {
  AppTheme {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = stringResource(R.string.vpnsetup_screen_title),
        style = AppTheme.typography.headlineLarge,
        color = AppTheme.colors.contentPrimary,
      )
      VSpacer(16.dp)
      HelpSectionHeading(stringResource(R.string.vpnsetup_steps_title))
      VSpacer(8.dp)
      HelpParagraph(stringResource(R.string.vpnsetup_intro))
      VSpacer(16.dp)
      VpnStep(number = 1, text = stringResource(R.string.vpnsetup_step_connect))
    }
  }
}

private data class Step(val stringRes: Int)

private val STEPS: List<Step> =
  listOf(
    Step(R.string.vpnsetup_step_source),
    Step(R.string.vpnsetup_step_profile),
    Step(R.string.vpnsetup_step_connect),
    Step(R.string.vpnsetup_step_confirm),
    Step(R.string.vpnsetup_step_keep),
  )

private val DEFAULT_STEPS: List<Step> =
  listOf(
    Step(R.string.vpnsetup_default_step_settings),
    Step(R.string.vpnsetup_default_step_network),
    Step(R.string.vpnsetup_default_step_always),
    Step(R.string.vpnsetup_default_step_toggle),
  )

private val BATTERY_STEPS: List<Step> =
  listOf(
    Step(R.string.vpnsetup_battery_step_settings),
    Step(R.string.vpnsetup_battery_step_apps),
    Step(R.string.vpnsetup_battery_step_optimize),
    Step(R.string.vpnsetup_battery_step_unrestricted),
  )
