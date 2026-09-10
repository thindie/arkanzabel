package com.thindie.rknzbl.appfeatures.settings.ui.source

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.thindie.engine.core.ScreenScope
import com.thindie.engine.uikit.Action
import com.thindie.engine.uikit.AppScreen
import com.thindie.engine.uikit.AppTheme
import com.thindie.engine.uikit.Button
import com.thindie.engine.uikit.SentenceRow
import com.thindie.engine.uikit.TextField
import com.thindie.engine.uikit.TopAppBar
import com.thindie.engine.uikit.VSpacer
import com.thindie.rknzbl.R

@Composable
internal fun SourceScreenContent(scope: ScreenScope<SourceState, SourceCommand>) {
  val state by scope.state.collectAsState()
  val focusRequester = remember { FocusRequester() }

  // Hoist string resources out of lambdas (not in composable context)
  val titleChoose = stringResource(R.string.home_choose_source)
  val sectionBlack = stringResource(R.string.source_black_vless_title)
  val sectionWhite = stringResource(R.string.source_white_cidr_title)
  val sectionCustom = stringResource(R.string.settings_custom_source_title)
  val subtitleAll = stringResource(R.string.source_subtitle_all_configs)
  val subtitleTop150 = stringResource(R.string.source_subtitle_top150_phone)
  val subtitleTop1501 = stringResource(R.string.source_subtitle_top150_phone_1)
  val subtitleTop1502 = stringResource(R.string.source_subtitle_top150_phone_2)
  val subtitleRuServices = stringResource(R.string.source_subtitle_ru_services)
  val titleBlackSs = stringResource(R.string.source_black_ss_title)
  val titleWhiteCidr = stringResource(R.string.source_white_cidr_title)
  val titleCustom = stringResource(R.string.source_custom_title)
  val btnSave = stringResource(R.string.settings_custom_source_save)

  AppScreen(
    screenScope = scope,
    modifier = Modifier.imePadding(),
  ) {
    BackHandler { scope.send(SourceCommand.Back) }
    TopAppBar(
      primary =
        Action(
          listener = { scope.send(SourceCommand.Back) },
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
        text = titleChoose,
        style = AppTheme.typography.headlineLarge,
        color = AppTheme.colors.contentPrimary,
      )

      // === Blacklist sources ===
      VSpacer(24.dp)
      Divider()
      VSpacer(16.dp)
      SectionTitle(sectionBlack)
      VSpacer(8.dp)

      SentenceRow(
        modifier = Modifier.fillMaxWidth(),
        painter = null,
        title = titleBlackSs,
        subtitle = subtitleAll,
        loading = false,
        onClick = {
          scope.send(
            SourceCommand.SelectPreset(
              "https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/refs/heads/main/BLACK_SS%2BAll_RUS.txt",
              titleBlackSs,
            ),
          )
        },
      )

      VSpacer(8.dp)
      SentenceRow(
        modifier = Modifier.fillMaxWidth(),
        painter = null,
        title = sectionBlack,
        subtitle = subtitleAll,
        loading = false,
        onClick = {
          scope.send(
            SourceCommand.SelectPreset(
              "https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/refs/heads/main/BLACK_VLESS_RUS.txt",
              sectionBlack,
            ),
          )
        },
      )

      VSpacer(8.dp)
      SentenceRow(
        modifier = Modifier.fillMaxWidth(),
        painter = null,
        title = sectionBlack,
        subtitle = subtitleTop150,
        loading = false,
        onClick = {
          scope.send(
            SourceCommand.SelectPreset(
              "https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/refs/heads/main/BLACK_VLESS_RUS_mobile.txt",
              sectionBlack,
            ),
          )
        },
      )

      // === Whitelist sources ===
      VSpacer(24.dp)
      Divider()
      VSpacer(16.dp)
      SectionTitle(sectionWhite)
      VSpacer(8.dp)

      SentenceRow(
        modifier = Modifier.fillMaxWidth(),
        painter = null,
        title = titleWhiteCidr,
        subtitle = subtitleAll,
        loading = false,
        onClick = {
          scope.send(
            SourceCommand.SelectPreset(
              "https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/refs/heads/main/WHITE-CIDR-RU-all.txt",
              titleWhiteCidr,
            ),
          )
        },
      )

      VSpacer(8.dp)
      SentenceRow(
        modifier = Modifier.fillMaxWidth(),
        painter = null,
        title = titleWhiteCidr,
        subtitle = subtitleTop1501,
        loading = false,
        onClick = {
          scope.send(
            SourceCommand.SelectPreset(
              "https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/refs/heads/main/Vless-Reality-White-Lists-Rus-Mobile.txt",
              titleWhiteCidr,
            ),
          )
        },
      )

      VSpacer(8.dp)
      SentenceRow(
        modifier = Modifier.fillMaxWidth(),
        painter = null,
        title = titleWhiteCidr,
        subtitle = subtitleTop1502,
        loading = false,
        onClick = {
          scope.send(
            SourceCommand.SelectPreset(
              "https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/refs/heads/main/Vless-Reality-White-Lists-Rus-Mobile-2.txt",
              titleWhiteCidr,
            ),
          )
        },
      )

      VSpacer(8.dp)
      SentenceRow(
        modifier = Modifier.fillMaxWidth(),
        painter = null,
        title = titleWhiteCidr,
        subtitle = subtitleRuServices,
        loading = false,
        onClick = {
          scope.send(
            SourceCommand.SelectPreset(
              "https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/refs/heads/main/WHITE-CIDR-RU-checked.txt",
              titleWhiteCidr,
            ),
          )
        },
      )

      // === Custom URL ===
      VSpacer(24.dp)
      Divider()
      VSpacer(16.dp)
      SectionTitle(sectionCustom)
      VSpacer(8.dp)

      TextField(
        modifier =
          Modifier
            .focusRequester(focusRequester)
            .fillMaxWidth(),
        value = state.customUrlInput,
        onValueChange = { scope.send(SourceCommand.SetCustomUrl(it)) },
        placeholder = stringResource(R.string.settings_custom_source_placeholder),
        showClearButton = true,
      )

      VSpacer(8.dp)
      Row(horizontalArrangement = Arrangement.End) {
        Button(
          text = btnSave,
          onClick = { scope.send(SourceCommand.SelectPreset(state.customUrlInput, titleCustom)) },
        )
      }

      VSpacer(32.dp)
    }

    LaunchedEffect(Unit) {
      focusRequester.requestFocus()
    }
  }
}

@Composable
private fun SectionTitle(text: String) {
  Text(
    text = text,
    style = AppTheme.typography.titleMedium,
    color = AppTheme.colors.contentSecondary,
  )
}
