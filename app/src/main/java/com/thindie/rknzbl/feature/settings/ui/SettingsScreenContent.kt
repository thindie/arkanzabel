package com.thindie.rknzbl.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.thindie.rknzbl.R
import com.thindie.rknzbl.engine.ScreenScope
import com.thindie.rknzbl.uikit.AppScreen
import com.thindie.rknzbl.uikit.AppTheme
import com.thindie.rknzbl.uikit.VSpacer

/**
 * Screen content for the Settings screen.
 * Handles UI rendering for theme mode selection and autosave toggle.
 */

@Composable
fun ScreenScope<SettingsScreenState, SettingsScreenCommand>.SettingsScreenContent() {
  AppScreen {
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(16.dp),
    ) {
      Text(
        text = stringResource(R.string.home_select_theme_title),
        style = AppTheme.typography.headlineLarge,
        color = AppTheme.colors.contentPrimary,
      )
      VSpacer(8.dp)

      // Theme mode selection
      // Auto theme option
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = stringResource(R.string.home_select_theme_auto_subtitle),
            style = AppTheme.typography.titleMedium,
            color = AppTheme.colors.contentPrimary,
          )
        }
        Switch(
          checked = state.themeMode == "auto",
          onCheckedChange = { send(SettingsScreenCommand.SetThemeMode(if (it) "auto" else null)) },
        )
      }

      // Light theme option
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = stringResource(R.string.home_select_theme_light_subtitle),
            style = AppTheme.typography.titleMedium,
            color = AppTheme.colors.contentPrimary,
          )
        }
        Switch(
          checked = state.themeMode == "light",
          onCheckedChange = { send(SettingsScreenCommand.SetThemeMode(if (it) "light" else null)) },
        )
      }

      // Dark theme option
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = stringResource(R.string.home_select_theme_dark_subtitle),
            style = AppTheme.typography.titleMedium,
            color = AppTheme.colors.contentPrimary,
          )
        }
        Switch(
          checked = state.themeMode == "dark",
          onCheckedChange = { send(SettingsScreenCommand.SetThemeMode(if (it) "dark" else null)) },
        )
      }

      VSpacer(24.dp)

      // Autosave toggle
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = stringResource(R.string.home_select_autosave_title),
            style = AppTheme.typography.titleMedium,
            color = AppTheme.colors.contentPrimary,
          )
          VSpacer(2.dp)
          Text(
            text = stringResource(R.string.home_select_autosave_subtitle),
            style = AppTheme.typography.bodySmall,
            color = AppTheme.colors.contentSecondary,
          )
        }
        Switch(
          checked = state.autosaveEnabled ?: true,
          onCheckedChange = { send(SettingsScreenCommand.ToggleAutosave) },
        )
      }
    }
  }
}
