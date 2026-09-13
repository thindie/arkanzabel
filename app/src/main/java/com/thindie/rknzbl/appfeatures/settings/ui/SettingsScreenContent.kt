package com.thindie.rknzbl.appfeatures.settings.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.thindie.engine.core.ScreenScope
import com.thindie.engine.core.Section
import com.thindie.engine.core.ServiceCommand
import com.thindie.engine.uikit.Action
import com.thindie.engine.uikit.AppScreen
import com.thindie.engine.uikit.AppTheme
import com.thindie.engine.uikit.HSpacer
import com.thindie.engine.uikit.LocalThemeSwitcher
import com.thindie.engine.uikit.ThemeSwitcher
import com.thindie.engine.uikit.Toggle
import com.thindie.engine.uikit.TopAppBar
import com.thindie.engine.uikit.VSpacer
import com.thindie.rknzbl.R
import com.v2ray.ang.dto.WebDavConfig

@Composable
internal fun SettingsScreenContent(scope: ScreenScope<ScreenState, ScreenCommand>) {
  AppScreen(scope) {
    val state by scope.state.collectAsState()
    val themeSwitcher = LocalThemeSwitcher.current
    val theme by themeSwitcher.themeFlow.collectAsState(ThemeSwitcher.Choice.Auto)
    BackHandler { scope.send(ScreenCommand.Back) }
    if (state.legacyRestart) {
      val context = LocalActivity.current
      LaunchedEffect(Unit) {
        context?.recreate()
      }
    }
    val showBack = state.section == Section.Leaf
    if (showBack) {
      TopAppBar(
        primary =
          Action(
            listener = { scope.send(ScreenCommand.Back) },
            resRef = R.drawable.ic_arrow_back_24,
          ),
      )
    } else {
      Unit
    }
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

      VSpacer(24.dp)
      SourceSelectorRow(
        label = stringResource(R.string.home_choose_source),
        subtitle = sourceDisplayName(state.customSourceUrl),
        selected = state.customSourceUrl != null,
        onClick = { scope.send(ScreenCommand.ToggleCustomSource) },
      )

      VSpacer(8.dp)
      SourceSelectorRow(
        label = stringResource(R.string.settings_webdav_title),
        subtitle = webDavDisplayName(state.webDavConfig),
        selected = state.webDavConfig != null,
        onClick = { scope.send(ScreenCommand.OpenWebdav) },
      )

      VSpacer(8.dp)
      SourceSelectorRow(
        label = stringResource(R.string.per_app_proxy_row_title),
        subtitle = stringResource(R.string.per_app_proxy_row_subtitle),
        selected = false,
        onClick = { scope.send(ScreenCommand.OpenPerAppProxy) },
      )

      // === Appearance ===
      VSpacer(24.dp)
      Divider()
      VSpacer(16.dp)
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
        onCheckedChange = { scope.send(ScreenCommand.ToggleAutosave) },
      )

      ToggleRow(
        label = stringResource(R.string.settings_use_new_design_title),
        subtitle = stringResource(R.string.settings_use_new_design_subtitle),
        checked = state.useNewDesign ?: false,
        onCheckedChange = { scope.send(ScreenCommand.ToggleNewDesign) },
      )

      ToggleRow(
        label = stringResource(R.string.settings_local_storage_title),
        subtitle = stringResource(R.string.settings_local_storage_subtitle),
        checked = state.isLocalSave ?: false,
        onCheckedChange = { scope.send(ScreenCommand.ToggleLocalStorage) },
      )

      ToggleRow(
        label = stringResource(R.string.settings_speed_notification_title),
        subtitle = stringResource(R.string.settings_speed_notification_subtitle),
        checked = state.speedEnabled ?: false,
        onCheckedChange = {
          if (state.speedEnabled != true) {
            scope.sendEvent(
              ServiceCommand.UiEvent.Decision(
                content = { SpeedNotificationRestartDialog() },
                primaryAction =
                  Action(
                    listener = { scope.send(ScreenCommand.ToggleSpeed) },
                    resRef = R.string.btn_close,
                  ),
              ),
            )
          } else {
            scope.send(ScreenCommand.ToggleSpeed)
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
        onClick = { selectLanguage(scope, state.language.orEmpty()) },
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
        onCheckedChange = { scope.send(ScreenCommand.ToggleMux) },
      )

      MuxFaqRow { sendMuxFaq(scope) }

      // === Help ===
      VSpacer(24.dp)
      Divider()
      VSpacer(16.dp)
      SectionTitle(stringResource(R.string.settings_section_help))
      VSpacer(16.dp)

      FaqPortalRow(
        label = stringResource(R.string.help_row_title),
        subtitle = stringResource(R.string.help_row_subtitle),
        onClick = { scope.send(ScreenCommand.OpenHelp) },
      )
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
    modifier =
      Modifier
        .fillMaxWidth()
        .clickable(enabled = enabled, onClick = onCheckedChange)
        .padding(12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = label,
        style = AppTheme.typography.titleMedium,
        color = AppTheme.colors.contentPrimary,
      )
      if (subtitle != null) {
        VSpacer(2.dp)
        Text(
          text = subtitle,
          style = AppTheme.typography.bodySmall,
          color = AppTheme.colors.contentSecondary,
        )
      }
    }
    Toggle(checked = checked, enabled = enabled)
    if (!enabled) {
      HSpacer(2.dp)
      Image(
        painter = painterResource(R.drawable.ic_lock_24),
        contentDescription = null,
        modifier = Modifier.padding(start = 8.dp),
      )
    }
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
    modifier =
      Modifier
        .fillMaxWidth()
        .clickable(
          onClick = onCheckedChange,
          indication = null,
          interactionSource = null,
        )
        .padding(12.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = label,
        style = AppTheme.typography.titleMedium,
        color = AppTheme.colors.contentPrimary,
      )
      VSpacer(2.dp)
      Text(
        text = subtitle,
        style = AppTheme.typography.bodySmall,
        color = AppTheme.colors.contentSecondary,
      )
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
    modifier =
      Modifier
        .fillMaxWidth()
        .clickable(onClick = onClick)
        .padding(12.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = label,
        style = AppTheme.typography.titleMedium,
        color = AppTheme.colors.contentPrimary,
      )
      VSpacer(2.dp)
      Text(
        text = subtitle,
        style = AppTheme.typography.bodySmall,
        color = AppTheme.colors.contentSecondary,
      )
    }
  }
}

@Composable
private fun MuxFaqRow(onClick: () -> Unit) {
  val faqTitle = stringResource(R.string.mux_faq_title)
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .clickable(onClick = onClick)
        .padding(12.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = faqTitle,
      style = AppTheme.typography.titleMedium,
      color = AppTheme.colors.contentPrimary,
    )
  }
}

private fun sendMuxFaq(scope: ScreenScope<*, *>) {
  scope.sendEvent(
    ServiceCommand.UiEvent.Decision(
      content = { MuxFaqDialog() },
      primaryAction = Action(listener = {}, resRef = R.string.mux_faq_ok),
    ),
  )
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

private fun selectLanguage(
  scope: ScreenScope<ScreenState, ScreenCommand>,
  currentLanguage: String,
) {
  scope.sendEvent(
    ServiceCommand.UiEvent.Decision(
      content = {
        LanguagePickerDialog(
          currentLanguage = currentLanguage,
          onClick = { code ->
            scope.send(ScreenCommand.SelectLanguage(code))
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
internal fun FaqPortalRow(
  label: String,
  subtitle: String,
  onClick: () -> Unit,
) {
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .clickable(onClick = onClick)
        .background(color = AppTheme.colors.cardPrimary, shape = RoundedCornerShape(20.dp))
        .padding(16.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = label,
        style = AppTheme.typography.titleMedium,
        color = AppTheme.colors.contentPrimary,
      )
      VSpacer(2.dp)
      Text(
        text = subtitle,
        style = AppTheme.typography.bodySmall,
        color = AppTheme.colors.contentSecondary,
      )
    }
    Image(
      painter = painterResource(R.drawable.ic_chevron_right_24),
      contentDescription = null,
      colorFilter = ColorFilter.tint(AppTheme.colors.contentSecondary),
    )
  }
}

@Preview(showBackground = true)
@Composable
private fun FaqPortalRowPreview() {
  AppTheme {
    FaqPortalRow(
      label = "FAQ",
      subtitle = "WebDAV, per-app proxy, notification legend and logs",
      onClick = {},
    )
  }
}

@Composable
private fun SourceSelectorRow(
  label: String,
  subtitle: String,
  selected: Boolean,
  onClick: () -> Unit,
) {
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .clickable(onClick = onClick)
        .background(
          color = if (selected) AppTheme.colors.buttonAccent else AppTheme.colors.cardPrimary,
          shape = RoundedCornerShape(20.dp),
        )
        .padding(16.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = label,
        style = AppTheme.typography.titleMedium,
        color = if (selected) AppTheme.colors.onButtonAccent else AppTheme.colors.contentPrimary,
      )
      VSpacer(2.dp)
      Text(
        text = subtitle,
        style = AppTheme.typography.bodySmall,
        color = if (selected) AppTheme.colors.onButtonAccent.copy(alpha = 0.8f) else AppTheme.colors.contentSecondary,
      )
    }
    Image(
      painter = painterResource(R.drawable.ic_chevron_right_24),
      contentDescription = null,
      colorFilter = ColorFilter.tint(if (selected) AppTheme.colors.onButtonAccent else AppTheme.colors.contentSecondary),
    )
  }
}

@Composable
private fun webDavDisplayName(config: WebDavConfig?): String {
  if (config == null || config.baseUrl.isBlank()) {
    return stringResource(R.string.settings_webdav_subtitle_off)
  }
  val url = config.baseUrl
  return if (url.length > 40) url.take(37) + "..." else url
}

@Composable
private fun sourceDisplayName(url: String?): String {
  val ctx = LocalContext.current
  if (url == null) return stringResource(R.string.settings_custom_source_subtitle_off)
  return when (url) {
    "https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/refs/heads/main/BLACK_SS%2BAll_RUS.txt" ->
      stringResource(R.string.source_black_ss_title)
    "https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/refs/heads/main/BLACK_VLESS_RUS.txt" ->
      stringResource(R.string.source_black_vless_title) + " (" + stringResource(R.string.source_subtitle_all_configs) + ")"
    "https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/refs/heads/main/BLACK_VLESS_RUS_mobile.txt" ->
      stringResource(R.string.source_black_vless_title) + " (" + stringResource(R.string.source_subtitle_top150_phone) + ")"
    "https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/refs/heads/main/WHITE-CIDR-RU-all.txt" ->
      stringResource(R.string.source_white_cidr_title) + " (" + stringResource(R.string.source_subtitle_all_configs) + ")"
    "https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/refs/heads/main/Vless-Reality-White-Lists-Rus-Mobile.txt" ->
      stringResource(R.string.source_white_cidr_title) + " (" + stringResource(R.string.source_subtitle_top150_phone_1) + ")"
    "https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/refs/heads/main/Vless-Reality-White-Lists-Rus-Mobile-2.txt" ->
      stringResource(R.string.source_white_cidr_title) + " (" + stringResource(R.string.source_subtitle_top150_phone_2) + ")"
    "https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/refs/heads/main/WHITE-CIDR-RU-checked.txt" ->
      stringResource(R.string.source_white_cidr_title) + " (" + stringResource(R.string.source_subtitle_ru_services) + ")"
    else -> {
      // Custom URL - show truncated
      if (url.length > 40) url.take(37) + "..." else url
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

@Composable
private fun SpeedNotificationRestartDialog() {
  Column {
    Text(
      text = stringResource(R.string.settings_speed_notification_restart_title),
      style = AppTheme.typography.headlineMedium,
      color = AppTheme.colors.contentPrimary,
    )
    VSpacer(16.dp)
    Text(
      text = stringResource(R.string.settings_speed_notification_restart_message),
      style = AppTheme.typography.bodyMedium,
      color = AppTheme.colors.contentPrimary,
    )
  }
}
