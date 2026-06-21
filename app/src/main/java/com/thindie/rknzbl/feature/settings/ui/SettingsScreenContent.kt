package com.thindie.rknzbl.feature.settings.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
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
import com.thindie.rknzbl.uikit.TopAppBar
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
    TopAppBar(
      primary =
        Action(
          listener = { send(ScreenCommand.Back) },
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
      if (theme == ThemeSwitcher.Choice.Auto) {
        VSpacer(8.dp)
        Text(
          text = stringResource(R.string.home_select_theme_auto_locked),
          style = AppTheme.typography.bodySmall,
          color = AppTheme.colors.contentSecondary,
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
        Row {
          Toggle(
            enabled = theme != ThemeSwitcher.Choice.Auto,
            checked = theme == ThemeSwitcher.Choice.Light,
          )
          if (theme == ThemeSwitcher.Choice.Auto) {
            Image(
              painter = painterResource(R.drawable.ic_lock_24),
              contentDescription = null,
              modifier = Modifier.padding(start = 8.dp),
            )
          }
        }
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
        Row {
          Toggle(
            enabled = theme != ThemeSwitcher.Choice.Auto,
            checked = theme == ThemeSwitcher.Choice.Dark,
          )
          if (theme == ThemeSwitcher.Choice.Auto) {
            Image(
              painter = painterResource(R.drawable.ic_lock_24),
              contentDescription = null,
              modifier = Modifier.padding(start = 8.dp),
            )
          }
        }
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
              selectLanguage(state.language)
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

      // Local storage mode toggle
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .clickable {
              if (state.isLocalSave == true) {
                sendEvent(
                  ServiceCommand.UiEvent.Decision(
                    content = {
                      Column {
                        Text(
                          text = stringResource(R.string.storage_mode_warning_title),
                          style = AppTheme.typography.headlineMedium,
                          color = AppTheme.colors.contentPrimary,
                        )
                        VSpacer(16.dp)
                        Text(
                          text = stringResource(R.string.storage_mode_warning_message),
                          style = AppTheme.typography.bodyMedium,
                          color = AppTheme.colors.contentPrimary,
                        )
                      }
                    },
                    primaryAction =
                      Action(listener = {
                        send(ScreenCommand.ToggleStorageMode)
                      }, resRef = R.string.storage_mode_warning_ok),
                  ),
                )
              } else {
                send(ScreenCommand.ToggleStorageMode)
              }
            }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = stringResource(R.string.home_select_storage_mode_title),
            style = AppTheme.typography.titleMedium,
            color = AppTheme.colors.contentPrimary,
          )
          VSpacer(2.dp)
          Text(
            text = stringResource(R.string.home_select_storage_mode_subtitle),
            style = AppTheme.typography.bodySmall,
            color = AppTheme.colors.contentSecondary,
          )
        }
        Toggle(checked = state.isLocalSave ?: false)
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

      // Start with favorite profiles toggle
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .clickable {
              send(ScreenCommand.StartWithFavoriteProfiles)
            }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = stringResource(R.string.settings_start_with_favorite_profiles_title),
            style = AppTheme.typography.titleMedium,
            color = AppTheme.colors.contentPrimary,
          )
          VSpacer(2.dp)
          Text(
            text = stringResource(R.string.settings_start_with_favorite_profiles_subtitle),
            style = AppTheme.typography.bodySmall,
            color = AppTheme.colors.contentSecondary,
          )
        }
        Toggle(
          checked = state.startWithFavoriteProfiles ?: false,
        )
      }
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
                        style = AppTheme.typography.headlineMedium,
                        color = AppTheme.colors.contentPrimary,
                      )
                      VSpacer(16.dp)
                      Text(
                        text = stringResource(R.string.mux_faq_body),
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colors.contentPrimary,
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

private fun ScreenScope<ScreenState, ScreenCommand>.selectLanguage(currentLanguage: String?) {
  sendEvent(
    ServiceCommand.UiEvent.Decision(
      content = {
        Column {
          Text(
            text = stringResource(R.string.settings_language_title),
            style = AppTheme.typography.headlineMedium,
            color = AppTheme.colors.contentPrimary,
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
              color = if (currentLanguage == "en") AppTheme.colors.contentPrimary else AppTheme.colors.contentSecondary,
            )
            if (currentLanguage == "en") {
              Text(
                text = "✓",
                style = AppTheme.typography.labelMedium,
                color = AppTheme.colors.accentPrimary,
              )
            }
          }
          VSpacer(24.dp)
          // Russian option
          Row(
            modifier = Modifier.clickable { send(ScreenCommand.SelectLanguage(languageCode = "ru")) },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = stringResource(R.string.language_ru),
              style = AppTheme.typography.bodyMedium,
              color = if (currentLanguage == "ru") AppTheme.colors.contentPrimary else AppTheme.colors.contentSecondary,
            )
            if (currentLanguage == "ru") {
              Text(
                text = "✓",
                style = AppTheme.typography.labelMedium,
                color = AppTheme.colors.accentPrimary,
              )
            }
          }
        }
      },
      primaryAction = Action(listener = {}, resRef = R.string.btn_close),
    ),
  )
}
