package com.thindie.rknzbl.feature.intro

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.thindie.rknzbl.R
import com.thindie.rknzbl.engine.ScreenScope
import com.thindie.rknzbl.uikit.AppScreen
import com.thindie.rknzbl.uikit.AppTheme
import com.thindie.rknzbl.uikit.Button
import com.thindie.rknzbl.uikit.SentenceRow

@Composable
internal fun IntroScreenContent(scope: ScreenScope<State, CommandIntro>) {
  val st by scope.state.collectAsState()
  val activity = LocalActivity.current
  val launcher =
    rememberLauncherForActivityResult(
      ActivityResultContracts.StartActivityForResult(),
    ) { result ->
      if (result.resultCode == Activity.RESULT_OK) {
        scope.send(CommandIntro.ConfirmRationale)
      } else {
        scope.send(CommandIntro.PermissionDenied)
      }
    }

  val permissionLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestPermission(),
    ) {
      if (activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) ==
          PackageManager.PERMISSION_GRANTED
        ) {
          scope.send(CommandIntro.ConfirmRationale)
        } else {
          scope.send(CommandIntro.PermissionDenied)
        }
      }
    }

  val settingsLauncher =
    rememberLauncherForActivityResult(
      ActivityResultContracts.StartActivityForResult(),
    ) {
      if (activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) ==
          PackageManager.PERMISSION_GRANTED
        ) {
          scope.send(CommandIntro.ConfirmRationale)
        }
      }
    }

  BackHandler { scope.send(CommandIntro.Dismiss) }

  AppScreen(
    scope = scope,
    title =
      stringResource(
        when (st.current) {
          Permission.Vpn -> R.string.intro_screen_title_vpn
          Permission.Push -> R.string.intro_screen_title_push
        },
      ),
    subtitle = st.hint,
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      when (st.stage) {
        Stage.Loading -> Unit

        Stage.SoftRequest -> {
          AlertDialog(
            containerColor = AppTheme.colors.backgroundPrimary,
            text = {
              SentenceRow(
                painter = painterResource(R.drawable.ic_attention_24),
                onClick = null,
                title =
                  stringResource(
                    when (st.current) {
                      Permission.Vpn -> R.string.intro_soft_title_vpn
                      Permission.Push -> R.string.intro_soft_title_push
                    },
                  ),
                subtitle =
                  stringResource(
                    when (st.current) {
                      Permission.Vpn -> R.string.intro_soft_subtitle_vpn
                      Permission.Push -> R.string.intro_soft_subtitle_push
                    },
                  ),
                loading = false,
              )
            },
            onDismissRequest = { },
            dismissButton = {
              Button(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.intro_btn_not_now),
                onClick = { scope.send(CommandIntro.DeclineSoftRequest) },
              )
            },
            confirmButton = {
              Button(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.intro_btn_understood),
                onClick = { scope.send(CommandIntro.AcceptSoftRequest) },
              )
            },
          )
        }

        Stage.Rationale -> {
          LaunchedEffect(st.stage, st.current) {
            when (st.current) {
              Permission.Vpn -> {
                val intent = if (activity != null) VpnService.prepare(activity) else null
                if (intent == null) {
                  scope.send(CommandIntro.ConfirmRationale)
                } else {
                  launcher.launch(intent)
                }
              }

              Permission.Push -> {
                if (activity != null) {
                  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.POST_NOTIFICATIONS,
                      ) != PackageManager.PERMISSION_GRANTED
                    ) {
                      permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                      scope.send(CommandIntro.ConfirmRationale)
                    }
                  }
                } else {
                  scope.send(CommandIntro.ConfirmRationale)
                }
              }
            }
          }
        }

        Stage.RationaleDismissedOnce -> {
          AlertDialog(
            containerColor = AppTheme.colors.backgroundPrimary,
            text = {
              SentenceRow(
                painter = painterResource(R.drawable.ic_attention_24),
                onClick = null,
                title = stringResource(R.string.intro_push_denied_title),
                subtitle = stringResource(R.string.intro_push_denied_subtitle),
                loading = false,
              )
            },
            onDismissRequest = { },
            dismissButton = {
              Button(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.intro_push_continue_without),
                onClick = { send(CommandIntro.ConfirmRationale) },
              )
            },
            confirmButton = {
              Button(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.intro_push_open_settings),
                onClick = {
                  activity ?: return@Button
                  val primary = activity.intentAppNotificationSettings()
                  val fallback = activity.intentApplicationDetailsSettings()
                  val target =
                    if (primary.resolveActivity(activity.packageManager) != null) primary else fallback
                  settingsLauncher.launch(target)
                },
              )
            },
          )
        }
      }
    }
  }
}

private fun Context.intentAppNotificationSettings(): Intent =
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
      putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    }
  } else {
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
      data = Uri.fromParts("package", packageName, null)
    }
  }

private fun Context.intentApplicationDetailsSettings(): Intent =
  Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
    data = Uri.fromParts("package", packageName, null)
  }
