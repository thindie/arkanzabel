package com.thindie.rknzbl.feature.settings.ui.inputurl

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.thindie.rknzbl.R
import com.thindie.rknzbl.engine.ScreenScope
import com.thindie.rknzbl.uikit.Action
import com.thindie.rknzbl.uikit.AppScreen
import com.thindie.rknzbl.uikit.AppTheme
import com.thindie.rknzbl.uikit.Button
import com.thindie.rknzbl.uikit.VSpacer
import com.thindie.rknzbl.uikit.WSpacer

@Composable
internal fun InputUrlScreenContent(scope: ScreenScope<InputUrlState, InputUrlCommand>) {
  val screenState by scope.state.collectAsState()
  val focusRequester = remember { FocusRequester() }
  AppScreen(
    modifier = Modifier.imePadding(),
    primary =
      Action(
        resRef = R.drawable.ic_arrow_back_24,
        listener = { scope.send(InputUrlCommand.Back) },
      ),
  ) {
    BackHandler { scope.send(InputUrlCommand.Back) }
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = stringResource(R.string.settings_custom_source_title),
        style = AppTheme.typography.headlineLarge,
        color = AppTheme.colors.contentPrimary,
      )
      Text(
        text = stringResource(R.string.settings_custom_source_subtitle_off),
        style = AppTheme.typography.labelMedium,
        color = AppTheme.colors.contentSecondary,
      )
      VSpacer(24.dp)
      BasicTextField(
        modifier =
          Modifier
            .focusRequester(focusRequester)
            .fillMaxWidth()
            .background(
              AppTheme.colors.backgroundSecondary,
              shape = RoundedCornerShape(16.dp),
            )
            .padding(16.dp),
        textStyle =
          TextStyle(
            color = AppTheme.colors.contentPrimary,
          ),
        value = screenState.url,
        onValueChange = { scope.send(InputUrlCommand.SetUrl(it)) },
      )
      WSpacer()
      Button(
        text = stringResource(R.string.mux_faq_ok),
        onClick = { scope.send(InputUrlCommand.Done) },
      )
    }

    LaunchedEffect(Unit) {
      focusRequester.requestFocus()
    }
    DisposableEffect(Unit) {
      onDispose { focusRequester.freeFocus() }
    }
  }
}
