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
        text = stringResource(R.string.home_select_settings_title),
        style = AppTheme.typography.headlineLarge,
        color = AppTheme.colors.contentPrimary,
      )

      // === Appearance ===
      VSpacer(24.dp)
      SectionTitle(stringResource(R.string.settings_section_appearance))
      VSpacer(16.dp)

      ThemeOption(
        label = stringResource(R.string.home_select_theme_auto_title),
        subtitle = stringResource(R.string.home_select_theme_auto_subtitle),
        checked = theme == ThemeSwitcher.Choice.Auto,
        onCheckedChange = {
          themeSwitcher.set(
            if (theme == ThemeSwitcher.Choice.Auto) ThemeSwitcher.Choice.Dark else ThemeSwitcher.Choice.Auto,
          )
        },
      )

      if (theme == ThemeSwitcher.Choice.Auto) {
        VSpacer(8.dp)
        Text(
          text = stringResource(R.string.home_select_theme_auto_locked),
          style = AppTheme.typography.bodySmall,
          color = AppTheme.colors.contentSecondary,
        )
      }

      ThemeOption(
        label = stringResource(R.string.home_select_theme_light_subtitle),
        checked = theme == ThemeSwitcher.Choice.Light,
        enabled = theme != ThemeSwitcher.Choice.Auto,
        onCheckedChange = {
          if (theme == ThemeSwitcher.Choice.Auto) return@ThemeOption
          themeSwitcher.set(ThemeSwitcher.Choice.Light)
        },
      )

      ThemeOption(
        label = stringResource(R.string.home_select_theme_dark_subtitle),
        checked = theme == ThemeSwitcher.Choice.Dark,
        enabled = theme != ThemeSwitcher.Choice.Auto,
        onCheckedChange = {
          if (theme == ThemeSwitcher.Choice.Auto) return@ThemeOption
          themeSwitcher.set(ThemeSwitcher.Choice.Dark)
        },
      )

      // === General ===
      VSpacer(24.dp)
      Divider()
      VSpacer(16.dp)
      SectionTitle(stringResource(R.string.settings_section_general))
      VSpacer(16.dp)

      ToggleRow(
        label = stringResource(R.string.home_select_autosave_title),
        subtitle = stringResource(R.string.home_select_autosave_subtitle),
        checked = state.autosaveEnabled ?: true,
        onCheckedChange = { send(ScreenCommand.ToggleAutosave) },
      )

      ToggleRow(
        label = stringResource(R.string.settings_start_with_favorite_profiles_title),
        subtitle = stringResource(R.string.settings_start_with_favorite_profiles_subtitle),
        checked = state.startWithFavoriteProfiles ?: false,
        onCheckedChange = { send(ScreenCommand.StartWithFavoriteProfiles) },
      )

      ToggleRow(
        label = stringResource(R.string.home_select_storage_mode_title),
        subtitle = stringResource(R.string.home_select_storage_mode_subtitle),
        checked = state.isLocalSave ?: false,
        onCheckedChange = {
          if (state.isLocalSave == false) {
            sendEvent(
              ServiceCommand.UiEvent.Decision(
                content = { StorageWarningDialog() },
                primaryAction =
                  Action(
                    listener = { send(ScreenCommand.ToggleStorageMode) },
                    resRef = R.string.storage_mode_warning_ok,
                  ),
              ),
            )
          } else {
            send(ScreenCommand.ToggleStorageMode)
          }
        },
      )

      // === Language ===
      VSpacer(24.dp)
      Divider()
      VSpacer(16.dp)
      SectionTitle(stringResource(R.string.settings_section_language))
      VSpacer(16.dp)

      LanguageSection(
        label = stringResource(R.string.settings_language_title),
        subtitle = languageLabel(state.language),
        onClick = { selectLanguage(state.language.orEmpty()) },
      )

      // === MUX ===
      VSpacer(24.dp)
      Divider()
      VSpacer(16.dp)
      SectionTitle(stringResource(R.string.settings_section_mux))
      VSpacer(16.dp)

      ToggleRow(
        label = stringResource(R.string.settings_mux_title),
        subtitle = stringResource(R.string.settings_mux_subtitle),
        checked = state.muxEnabled ?: false,
        onCheckedChange = { send(ScreenCommand.ToggleMux) },
      )

      MuxFaqRow { sendMuxFaq() }
    }
  }
}

// === Helper composables ===

@Composable
private fun SectionTitle(text: String) {
  Text(
    text = text,
    style = AppTheme.typography.titleMedium,
    color = AppTheme.colors.contentSecondary,
  )
}

@Composable
private fun ThemeOption(
  label: String,
  checked: Boolean,
  enabled: Boolean = true,
  subtitle: String? = null,
  onCheckedChange: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onCheckedChange).padding(12.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(text = label, style = AppTheme.typography.titleMedium, color = AppTheme.colors.contentPrimary)
      if (subtitle != null) {
        VSpacer(2.dp)
        Text(text = subtitle, style = AppTheme.typography.bodySmall, color = AppTheme.colors.contentSecondary)
      }
    }
    Toggle(checked = checked, enabled = enabled)
  }
}

@Composable
private fun ToggleRow(
  label: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().clickable(onClick = onCheckedChange).padding(12.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(text = label, style = AppTheme.typography.titleMedium, color = AppTheme.colors.contentPrimary)
      VSpacer(2.dp)
      Text(text = subtitle, style = AppTheme.typography.bodySmall, color = AppTheme.colors.contentSecondary)
    }
    Toggle(checked = checked)
  }
}

@Composable
private fun LanguageSection(
  label: String,
  subtitle: String,
  onClick: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(12.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(text = label, style = AppTheme.typography.titleMedium, color = AppTheme.colors.contentPrimary)
      VSpacer(2.dp)
      Text(text = subtitle, style = AppTheme.typography.bodySmall, color = AppTheme.colors.contentSecondary)
    }
  }
}

@Composable
private fun MuxFaqRow(onClick: () -> Unit) {
  val faqTitle = stringResource(R.string.mux_faq_title)
  Row(
    modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(12.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(text = faqTitle, style = AppTheme.typography.titleMedium, color = AppTheme.colors.contentPrimary)
  }
}

private fun ScreenScope<*, *>.sendMuxFaq() {
  sendEvent(
    ServiceCommand.UiEvent.Decision(
      content = { MuxFaqDialog() },
      primaryAction = Action(listener = {}, resRef = R.string.mux_faq_ok),
    ),
  )
}

@Composable
private fun StorageWarningDialog() {
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
}

@Composable
private fun MuxFaqDialog() {
  Column {
    Text(
      text = stringResource(R.string.mux_faq_title),
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
}

private fun ScreenScope<ScreenState, ScreenCommand>.selectLanguage(currentLanguage: String) {
  sendEvent(
    ServiceCommand.UiEvent.Decision(
      content = {
        LanguagePickerDialog(
          currentLanguage = currentLanguage,
          onClick = { code ->
            send(ScreenCommand.SelectLanguage(code))
          },
        )
      },
      primaryAction = Action(listener = { }, resRef = R.string.btn_close),
    ),
  )
}

@Composable
private fun LanguagePickerDialog(
  currentLanguage: String,
  onClick: (String) -> Unit,
) {
  Column {
    Text(
      text = stringResource(R.string.settings_language_title),
      style = AppTheme.typography.headlineMedium,
      color = AppTheme.colors.contentPrimary,
    )
    VSpacer(24.dp)

    LanguageOption("en", currentLanguage) {
      onClick("en")
    }
    VSpacer(24.dp)
    LanguageOption("ru", currentLanguage) {
      onClick("ru")
    }
  }
}

@Composable
private fun LanguageOption(
  languageCode: String,
  currentLanguage: String?,
  onClick: () -> Unit,
) {
  Row(
    modifier = Modifier.clickable { onClick.invoke() },
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    val isSelected = currentLanguage == languageCode
    Text(
      text =
        when (languageCode) {
          "en" -> stringResource(R.string.language_en)
          "ru" -> stringResource(R.string.language_ru)
          else -> error("Unsupported locale")
        },
      style = AppTheme.typography.bodyMedium,
      color = if (isSelected) AppTheme.colors.contentPrimary else AppTheme.colors.contentSecondary,
    )
    if (isSelected) {
      Text(
        text = "✓",
        style = AppTheme.typography.labelMedium,
        color = AppTheme.colors.accentPrimary,
      )
    }
  }
}

@Composable
private fun languageLabel(language: String?): String {
  return when (language) {
    "en" -> stringResource(R.string.language_en)
    "ru" -> stringResource(R.string.language_ru)
    else -> language ?: ""
  }
}
