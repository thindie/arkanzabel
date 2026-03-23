package com.thindie.rknzbl.feature.home

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.thindie.rknzbl.R
import com.thindie.rknzbl.application.Application
import com.thindie.rknzbl.engine.ScreenScope
import com.thindie.rknzbl.engine.WorkState
import com.thindie.rknzbl.uikit.AppTheme
import com.thindie.rknzbl.uikit.Button
import com.thindie.rknzbl.uikit.VSpacer
import com.v2ray.ang.AppConfig
import com.v2ray.ang.util.MessageUtil

private sealed interface HomeVpnStartPlan {
  data class NeedUserMessage(val message: String) : HomeVpnStartPlan
  data class NeedVpnConsent(val intent: Intent) : HomeVpnStartPlan
  data object CanStartImmediately : HomeVpnStartPlan
}

/**
 * Resolves what to do before starting VPN: show error text, system consent screen, or start right away.
 * [needActivityMessage] must come from [stringResource] at the call site.
 */
private fun planHomeVpnStart(
  activity: ComponentActivity?,
  needActivityMessage: String,
): HomeVpnStartPlan {
  val act = activity ?: return HomeVpnStartPlan.NeedUserMessage(needActivityMessage)
  val intent = VpnService.prepare(act)
  return if (intent != null) HomeVpnStartPlan.NeedVpnConsent(intent)
  else HomeVpnStartPlan.CanStartImmediately
}

/**
 * [VpnService.prepare] + activity result stay here by platform contract.
 *
 * Core "running" state is updated by the VPN daemon process via `MSG_STATE_RUNNING` /
 * `MSG_STATE_NOT_RUNNING` broadcasts (no local polling).
 */
@Composable
fun ScreenScope<HomeFlow.State, HomeFlow.HomeCommand>.HomeVpnMvpPanel() {
  val context = LocalContext.current
  val st by state
  val hint = st.hint
  var uriDraft by remember { mutableStateOf("") }
  val app = context.applicationContext as? Application
  val runtimeState = app?.vpnRuntimeState?.collectAsState(WorkState.Idle)

  val activity = context as? ComponentActivity

  LaunchedEffect(runtimeState) {
    // Sync UI hint with daemon-side Error state.
    when (val s = runtimeState?.value) {
      is WorkState.Error -> send(HomeFlow.HomeCommand.SetMessage(s.message))
      else -> Unit
    }
  }

  LaunchedEffect(Unit) {
    // Ask the daemon process to report its current core running state.
    MessageUtil.sendMsg2Service(context, AppConfig.MSG_REGISTER_CLIENT, "")
  }

  DisposableEffect(Unit) {
    onDispose {
      // Avoid stale UI state when user navigates away.
      MessageUtil.sendMsg2Service(context, AppConfig.MSG_UNREGISTER_CLIENT, "")
    }
  }

  val statusText = if (runtimeState?.value is WorkState.Running) {
    stringResource(R.string.vpn_running_no_remarks)
  } else {
    stringResource(R.string.vpn_idle)
  }

  val vpnPermissionDeniedMessage = stringResource(R.string.vpn_permission_denied)
  val prepareLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.StartActivityForResult(),
  ) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
      send(HomeFlow.HomeCommand.VpnStartAfterPrepare)
    } else {
      send(HomeFlow.HomeCommand.SetMessage(vpnPermissionDeniedMessage))
    }
  }

  val proc = processing.value

  val needActivityMessage = stringResource(R.string.vpn_need_activity)

  fun requestStartVpn() {
    when (val plan = planHomeVpnStart(activity, needActivityMessage)) {
      is HomeVpnStartPlan.NeedUserMessage ->
        send(HomeFlow.HomeCommand.SetMessage(plan.message))
      is HomeVpnStartPlan.NeedVpnConsent ->
        prepareLauncher.launch(plan.intent)
      HomeVpnStartPlan.CanStartImmediately ->
        send(HomeFlow.HomeCommand.VpnStartAfterPrepare)
    }
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
  ) {
    Text(
      text = stringResource(R.string.vpn_mvp_hint),
      style = AppTheme.typography.bodySmall,
      color = AppTheme.colors.contentSecondary,
    )
    VSpacer(12.dp)
    OutlinedTextField(
      value = uriDraft,
      onValueChange = { uriDraft = it },
      modifier = Modifier.fillMaxWidth(),
      minLines = 3,
      textStyle = AppTheme.typography.bodySmall,
    )
    VSpacer(12.dp)
    Button(
      modifier = Modifier.fillMaxWidth(),
      text = stringResource(R.string.vpn_import),
      loading = proc is HomeFlow.HomeCommand.ImportProfile,
      onClick = { send(HomeFlow.HomeCommand.ImportProfile(uriDraft)) },
    )
    VSpacer(8.dp)
    Button(
      modifier = Modifier.fillMaxWidth(),
      text = stringResource(R.string.vpn_start),
      loading = proc is HomeFlow.HomeCommand.VpnStartAfterPrepare,
      onClick = { requestStartVpn() },
    )
    VSpacer(8.dp)
    Button(
      modifier = Modifier.fillMaxWidth(),
      text = stringResource(R.string.vpn_stop),
      loading = proc is HomeFlow.HomeCommand.StopVpn,
      onClick = { send(HomeFlow.HomeCommand.StopVpn) },
    )
    VSpacer(16.dp)
    Text(
      text = statusText,
      style = AppTheme.typography.titleMedium,
      color = AppTheme.colors.contentPrimary,
    )
    if (hint != null) {
      VSpacer(8.dp)
      Text(
        text = hint,
        style = AppTheme.typography.bodyMedium,
        color = AppTheme.colors.accentPrimary,
      )
    }
  }
}
