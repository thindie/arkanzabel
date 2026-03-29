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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.thindie.rknzbl.R
import com.thindie.rknzbl.engine.Command
import com.thindie.rknzbl.engine.Route
import com.thindie.rknzbl.engine.RouteFactory
import com.thindie.rknzbl.engine.Router
import com.thindie.rknzbl.engine.ScreenFlow
import com.thindie.rknzbl.engine.ScreenScope
import com.thindie.rknzbl.feature.home.HomeFlow
import com.thindie.rknzbl.uikit.AppScreen
import com.thindie.rknzbl.uikit.AppTheme
import com.thindie.rknzbl.uikit.Button
import com.thindie.rknzbl.uikit.SentenceRow

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

class IntroFlow(
  private val router: Router,
  val hasPushPermission: Boolean,
  private val appContext: Context,
) : ScreenFlow<Route, IntroFlow.Result>(router) {

  fun startAppFlow() {
    HomeFlow(router = router, appContext = appContext)
      .onFinishBuilder { finish(Result.Success) }
      .start()
  }

  enum class Result {
    Success,
  }

  override fun start() {
    router.push(main())
  }

  private fun hasVpnPermission(): Boolean = VpnService.prepare(appContext) == null

  fun main() = RouteFactory.create(
    initialState = State(),
    execute = ::exec,
    routeContent = { IntroScreenContent(this) },
    initialCommand = RouteFactory.InitialCommand {
      CommandIntro.Start as CommandIntro
    },
  )

  @Immutable
  data class State(
    val permissionScope: List<Permission> = buildList {
      add(Permission.Vpn)
      add(Permission.Push)
    },
    val permit: List<Permission> = emptyList(),
    val current: Permission = Permission.Vpn,
    val stage: Stage = Stage.Loading,
    val hint: String? = null,
  ) : com.thindie.rknzbl.engine.State

  enum class Stage {
    Loading,
    SoftRequest,
    Rationale,

    RationaleDismissedOnce,
  }

  enum class Permission {
    Vpn,
    Push,
  }

  sealed interface CommandIntro : Command {
    data object Start : CommandIntro
    data object AcceptSoftRequest : CommandIntro
    data object DeclineSoftRequest : CommandIntro
    data object ConfirmRationale : CommandIntro
    data object PushRestrictedOnce : CommandIntro
    data object PermissionDenied : CommandIntro
  }

  private suspend fun exec(command: CommandIntro, state: State): State {
    return when (command) {
      CommandIntro.Start -> {
        when (state.current) {
          Permission.Vpn -> {
            if (hasVpnPermission()) {
              state.copy(
                current = Permission.Push,
                stage = Stage.SoftRequest,
              )
            } else {
              state.copy(
                stage = Stage.SoftRequest,
                hint = null
              )
            }
          }

          Permission.Push -> {
            if (hasPushPermission) {
              startAppFlow()
              state
            } else {
              state.copy(
                stage = Stage.SoftRequest,
                hint = null
              )
            }
          }
        }
      }

      CommandIntro.AcceptSoftRequest -> {
        state.copy(
          stage = Stage.Rationale,
          hint = null
        )
      }

      CommandIntro.DeclineSoftRequest -> {
        state.copy(
          stage = Stage.SoftRequest,
          hint = when (state.current) {
            Permission.Vpn -> appContext.getString(R.string.intro_hint_vpn_decline)
            Permission.Push -> appContext.getString(R.string.intro_hint_push_decline)
          }
        )
      }

      CommandIntro.ConfirmRationale -> {
        when (state.current) {
          Permission.Vpn -> {
            if (hasVpnPermission()) {
              state.copy(
                current = Permission.Push,
                stage = Stage.SoftRequest,
              )
            } else {
              state.copy(
                stage = Stage.SoftRequest,
                hint = appContext.getString(R.string.intro_hint_vpn_not_granted)
              )
            }
          }

          Permission.Push -> {
            startAppFlow()
            state
          }
        }
      }

      CommandIntro.PermissionDenied -> {
        state.copy(
          stage = Stage.SoftRequest,
          hint = appContext.getString(R.string.intro_hint_vpn_not_granted)
        )
      }

      CommandIntro.PushRestrictedOnce -> {
        state.copy(
          stage = Stage.RationaleDismissedOnce
        )
      }
    }
  }
}

@Composable
private fun IntroScreenContent(scope: ScreenScope<IntroFlow.State, IntroFlow.CommandIntro>) =
  with(scope) {
    val st by state.collectAsState()
    val activity = LocalActivity.current
    val launcher = rememberLauncherForActivityResult(
      ActivityResultContracts.StartActivityForResult()
    ) { result ->
      if (result.resultCode == Activity.RESULT_OK) {
        send(IntroFlow.CommandIntro.ConfirmRationale)
      } else {
        send(IntroFlow.CommandIntro.PermissionDenied)
      }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
      if (isGranted) {
        send(IntroFlow.CommandIntro.ConfirmRationale)
      } else {
        val showRationale =
          activity?.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
            ?: false
        if (showRationale) {
          send(IntroFlow.CommandIntro.AcceptSoftRequest)
        } else {
          send(IntroFlow.CommandIntro.PushRestrictedOnce)
        }
      }
    }

    val settingsLauncher = rememberLauncherForActivityResult(
      ActivityResultContracts.StartActivityForResult()
    ) {
      val act = activity
      if (act != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(act, Manifest.permission.POST_NOTIFICATIONS) ==
          PackageManager.PERMISSION_GRANTED
        ) {
          send(IntroFlow.CommandIntro.ConfirmRationale)
        }
      }
    }

    BackHandler { }

    AppScreen(
      title = stringResource(
        when (st.current) {
          IntroFlow.Permission.Vpn -> R.string.intro_screen_title_vpn
          IntroFlow.Permission.Push -> R.string.intro_screen_title_push
        }
      ),
      subtitle = st.hint,
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
      ) {
        when (st.stage) {
          IntroFlow.Stage.Loading -> Unit
          IntroFlow.Stage.SoftRequest -> {
            AlertDialog(
              containerColor = AppTheme.colors.backgroundPrimary,
              text = {
                SentenceRow(
                  painter = painterResource(R.drawable.ic_attention_24),
                  onClick = null,
                  title = stringResource(
                    when (st.current) {
                      IntroFlow.Permission.Vpn -> R.string.intro_soft_title_vpn
                      IntroFlow.Permission.Push -> R.string.intro_soft_title_push
                    }
                  ),
                  subtitle = stringResource(
                    when (st.current) {
                      IntroFlow.Permission.Vpn -> R.string.intro_soft_subtitle_vpn
                      IntroFlow.Permission.Push -> R.string.intro_soft_subtitle_push
                    }
                  ),
                  loading = false,
                )
              },
              onDismissRequest = { },
              dismissButton = {
                Button(
                  modifier = Modifier.fillMaxWidth(),
                  text = stringResource(R.string.intro_btn_not_now),
                  onClick = { send(IntroFlow.CommandIntro.DeclineSoftRequest) }
                )
              },
              confirmButton = {
                Button(
                  modifier = Modifier.fillMaxWidth(),
                  text = stringResource(R.string.intro_btn_understood),
                  onClick = { send(IntroFlow.CommandIntro.AcceptSoftRequest) }
                )
              }
            )
          }

          IntroFlow.Stage.Rationale -> {
            LaunchedEffect(st.stage, st.current) {
              when (st.current) {
                IntroFlow.Permission.Vpn -> {
                  val intent = if (activity != null) {
                    VpnService.prepare(activity)
                  } else {
                    null
                  }
                  if (intent == null) {
                    send(IntroFlow.CommandIntro.ConfirmRationale)
                  } else {
                    launcher.launch(intent)
                  }
                }

                IntroFlow.Permission.Push -> {
                  if (activity != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                      if (ContextCompat.checkSelfPermission(
                          activity,
                          Manifest.permission.POST_NOTIFICATIONS
                        ) !=
                        PackageManager.PERMISSION_GRANTED
                      ) {
                        val showRationale =
                          activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
                        if (showRationale) {
                          permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                          send(IntroFlow.CommandIntro.PushRestrictedOnce)
                        }
                      } else {
                        send(IntroFlow.CommandIntro.ConfirmRationale)
                      }
                    }
                  }
                }
              }
            }
          }

          IntroFlow.Stage.RationaleDismissedOnce -> {
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
                  onClick = { send(IntroFlow.CommandIntro.ConfirmRationale) }
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
                    val pm = activity.packageManager
                    val target =
                      if (primary.resolveActivity(pm) != null) primary else fallback
                    settingsLauncher.launch(target)
                  }
                )
              }
            )
          }
        }
      }
    }
  }
