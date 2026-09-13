package com.thindie.rknzbl.appfeatures.settings.ui.webdav

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
import com.thindie.engine.uikit.HSpacer
import com.thindie.engine.uikit.TextField
import com.thindie.engine.uikit.TopAppBar
import com.thindie.engine.uikit.VSpacer
import com.thindie.rknzbl.R

@Composable
internal fun WebdavScreenContent(scope: ScreenScope<WebdavState, WebdavCommand>) {
  val state by scope.state.collectAsState()
  val focusRequester = remember { FocusRequester() }

  val title = stringResource(R.string.settings_webdav_title)
  val subtitle = stringResource(R.string.settings_webdav_subtitle)
  val placeholderUrl = stringResource(R.string.settings_webdav_url_placeholder)
  val placeholderUser = stringResource(R.string.settings_webdav_username_placeholder)
  val placeholderPass = stringResource(R.string.settings_webdav_password_placeholder)
  val btnSave = stringResource(R.string.settings_webdav_save)
  val btnClear = stringResource(R.string.settings_webdav_clear)

  AppScreen(
    screenScope = scope,
    modifier = Modifier.imePadding(),
  ) {
    BackHandler { scope.send(WebdavCommand.Back) }
    TopAppBar(
      primary =
        Action(
          listener = { scope.send(WebdavCommand.Back) },
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
        text = title,
        style = AppTheme.typography.headlineLarge,
        color = AppTheme.colors.contentPrimary,
      )

      VSpacer(8.dp)
      Text(
        text = subtitle,
        style = AppTheme.typography.bodySmall,
        color = AppTheme.colors.contentSecondary,
      )
      Column(
        modifier = Modifier.padding(horizontal = 16.dp),
      ) {
        VSpacer(24.dp)
        Divider()
        VSpacer(16.dp)

        TextField(
          modifier =
            Modifier
              .focusRequester(focusRequester)
              .fillMaxWidth(),
          value = state.urlInput,
          onValueChange = { scope.send(WebdavCommand.SetUrl(it)) },
          placeholder = placeholderUrl,
          showClearButton = true,
        )

        VSpacer(8.dp)
        TextField(
          modifier = Modifier.fillMaxWidth(),
          value = state.usernameInput,
          onValueChange = { scope.send(WebdavCommand.SetUsername(it)) },
          placeholder = placeholderUser,
          showClearButton = true,
        )

        VSpacer(8.dp)
        TextField(
          modifier = Modifier.fillMaxWidth(),
          value = state.passwordInput,
          onValueChange = { scope.send(WebdavCommand.SetPassword(it)) },
          placeholder = placeholderPass,
          showClearButton = true,
        )

        VSpacer(32.dp)
        Row(horizontalArrangement = Arrangement.End) {
          Button(text = btnClear, onClick = { scope.send(WebdavCommand.Clear) })
          HSpacer(8.dp)
          Button(text = btnSave, onClick = { scope.send(WebdavCommand.Save) })
        }
      }
    }
  }
}
