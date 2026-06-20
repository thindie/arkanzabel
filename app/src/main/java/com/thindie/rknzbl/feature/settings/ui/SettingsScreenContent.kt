package com.thindie.rknzbl.feature.settings.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.thindie.rknzbl.R
import com.thindie.rknzbl.engine.ScreenScope
import com.thindie.rknzbl.engine.ServiceCommand
import com.thindie.rknzbl.uikit.Action
import com.thindie.rknzbl.uikit.AppScreen
import com.thindie.rknzbl.uikit.AppTheme
import com.thindie.rknzbl.uikit.LocalThemeSwitcher
import com.thindie.rknzbl.uikit.ThemeSwitcher
import com.thindie.rknzbl.uikit.Toggle
import com.thindie.rknzbl.uikit.VSpacer

@Composable
internal fun ScreenScope<ScreenState, ScreenCommand>.SettingsScreenContent() {
  AppScreen {
    val state by state.collectAsState()
    val themeSwitcher = LocalThemeSwitcher.current
    val theme by themeSwitcher.themeFlow.collectAsState(ThemeSwitcher.Choice.Auto)
    BackHandler { send(ScreenCommand.Back) }
    if (state.legacyRestart) {
      val context = LocalActivity.current
      LaunchedEffect(Unit) {
        context?.recreate()
      }
    }
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(16.dp),
    ) {
      Text(
        text = stringResource(R.string.home_select_settings),
        style = AppTheme.typography.headlineLarge,
        color = AppTheme.colors.contentPrimary,
      )
      VSpacer(24.dp)

      // Theme mode selection
      // Auto theme option
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .clickable {
              themeSwitcher.set(
                if (theme == ThemeSwitcher.Choice.Auto) ThemeSwitcher.Choice.Dark else ThemeSwitcher.Choice.Auto,
              )
            }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = stringResource(R.string.home_select_theme_auto_title),
            style = AppTheme.typography.titleMedium,
            color = AppTheme.colors.contentPrimary,
          )
          VSpacer(2.dp)
          Text(
            text = stringResource(R.string.home_select_theme_auto_subtitle),
            style = AppTheme.typography.bodySmall,
            color = AppTheme.colors.contentSecondary,
          )
        }
        Toggle(
          checked = theme == ThemeSwitcher.Choice.Auto,
        )
      }
      VSpacer(16.dp)
      // Light theme option
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .clickable {
              if (theme == ThemeSwitcher.Choice.Auto) return@clickable
              themeSwitcher.set(ThemeSwitcher.Choice.Light)
            }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          modifier = Modifier.weight(1f),
          text = stringResource(R.string.home_select_theme_light_subtitle),
          style = AppTheme.typography.titleMedium,
          color = AppTheme.colors.contentPrimary,
        )
        Toggle(
          enabled = theme != ThemeSwitcher.Choice.Auto,
          checked = theme == ThemeSwitcher.Choice.Light,
        )
      }
      VSpacer(16.dp)
      // Dark theme option
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .clickable {
              if (theme == ThemeSwitcher.Choice.Auto) return@clickable
              themeSwitcher.set(ThemeSwitcher.Choice.Dark)
            }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          modifier = Modifier.weight(1f),
          text = stringResource(R.string.home_select_theme_dark_subtitle),
          style = AppTheme.typography.titleMedium,
          color = AppTheme.colors.contentPrimary,
        )
        Toggle(
          enabled = theme != ThemeSwitcher.Choice.Auto,
          checked = theme == ThemeSwitcher.Choice.Dark,
        )
      }

      VSpacer(24.dp)
      Divider()
      VSpacer(16.dp)

      // Autosave toggle
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .clickable {
              send(ScreenCommand.ToggleAutosave)
            }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
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
        Toggle(
          checked = state.autosaveEnabled ?: true,
        )
      }

      VSpacer(16.dp)
      Divider()
      VSpacer(16.dp)

      // Language selection
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .clickable {
              selectLanguage()
            }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = stringResource(R.string.settings_language_title),
            style = AppTheme.typography.titleMedium,
            color = AppTheme.colors.contentPrimary,
          )
          VSpacer(2.dp)
          Text(
            text =
              when (state.language) {
                "en" -> stringResource(R.string.language_en)
                "ru" -> stringResource(R.string.language_ru)
                else -> state.language ?: ""
              },
            style = AppTheme.typography.bodySmall,
            color = AppTheme.colors.contentSecondary,
          )
        }
      }

      VSpacer(16.dp)
      Divider()
      VSpacer(16.dp)

      // MUX toggle
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .clickable {
              send(ScreenCommand.ToggleMux)
            }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = stringResource(R.string.settings_mux_title),
            style = AppTheme.typography.titleMedium,
            color = AppTheme.colors.contentPrimary,
          )
          VSpacer(2.dp)
          Text(
            text = stringResource(R.string.settings_mux_subtitle),
            style = AppTheme.typography.bodySmall,
            color = AppTheme.colors.contentSecondary,
          )
        }
        Toggle(checked = state.muxEnabled ?: false)
      }

      VSpacer(16.dp)
      Divider()
      VSpacer(16.dp)

      // FAQ button
      val faqTitle = stringResource(R.string.mux_faq_title)
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .clickable {
              sendEvent(
                ServiceCommand.UiEvent.Decision(
                  content = {
                    Column {
                      Text(
                        text = faqTitle,
                        style = AppTheme.typography.titleMedium,
                      )
                      VSpacer(16.dp)
                      Text(
                        text = stringResource(R.string.mux_faq_body),
                        style = AppTheme.typography.bodyMedium,
                      )
                    }
                  },
                  primaryAction = Action(listener = {}, resRef = R.string.mux_faq_ok),
                ),
              )
            }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = faqTitle,
            style = AppTheme.typography.titleMedium,
            color = AppTheme.colors.contentPrimary,
          )
        }
      }
    }
  }
}

private fun ScreenScope<ScreenState, ScreenCommand>.selectLanguage() {
  sendEvent(
    ServiceCommand.UiEvent.Decision(
      content = {
        Column {
          Text(
            text = stringResource(R.string.settings_language_title),
            style = AppTheme.typography.titleMedium,
          )
          VSpacer(24.dp)

          // English option
          Row(
            modifier = Modifier.clickable { send(ScreenCommand.SelectLanguage(languageCode = "en")) },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = stringResource(R.string.language_en),
              style = AppTheme.typography.bodyMedium,
            )
          }
          VSpacer(8.dp)
          // Russian option
          Row(
            modifier = Modifier.clickable { send(ScreenCommand.SelectLanguage(languageCode = "ru")) },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = stringResource(R.string.language_ru),
              style = AppTheme.typography.bodyMedium,
            )
          }
        }
      },
      primaryAction = Action(listener = {}, resRef = R.string.btn_close),
    ),
  )
}
