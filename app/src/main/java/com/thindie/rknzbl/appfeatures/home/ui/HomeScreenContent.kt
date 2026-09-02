package com.thindie.rknzbl.appfeatures.home.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.thindie.engine.core.ScreenScope
import com.thindie.engine.core.WorkState
import com.thindie.engine.uikit.AppTheme
import com.thindie.rknzbl.R
import com.v2ray.ang.dto.ConnectionProfile
import com.v2ray.ang.enums.Protocol

/**
 * Simple home screen: one big connect/disconnect button with pulse animation when connected.
 */
@Composable
internal fun HomeScreenContent(scope: ScreenScope<ScreenState, ScreenCommand>) {
  val state = scope.state.collectAsState().value

  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Status text above button
      if (state.workState is WorkState.Running) {
        Text(
          text = stringResource(R.string.home_connected),
          style = AppTheme.typography.labelMedium,
          color = AppTheme.colors.successPrimary,
        )
        state.connectedProfile?.let { profile ->
          Text(
            text = profile.remarks.ifEmpty { "${profile.server}:${profile.serverPort}" },
            style = AppTheme.typography.bodySmall,
            color = AppTheme.colors.contentSecondary,
          )
        }
      }

      // Big pulsing button
      ConnectButton(state = state, onClick = { scope.send(ScreenCommand.ToggleConnect) })

      // Error message
      AnimatedVisibility(
        visible = state.workState is WorkState.Error,
        enter = fadeIn(),
        exit = fadeOut(),
      ) {
        val error = state.workState as? WorkState.Error
        Text(
          text = error?.message ?: "",
          style = AppTheme.typography.labelMedium,
          color = AppTheme.colors.errorPrimary,
        )
      }

      // Hint below
      if (state.workState is WorkState.Idle) {
        Text(
          text = stringResource(R.string.home_tap_to_connect),
          style = AppTheme.typography.labelMedium,
          color = AppTheme.colors.contentSecondary,
        )
      }
    }
  }
}

@Composable
internal fun ConnectButton(
  state: ScreenState,
  onClick: () -> Unit,
) {
  val isConnected = state.workState is WorkState.Running && state.connectedProfile != null
  val isConnecting = state.workState is WorkState.Running && state.connectedProfile == null

  // Fix: animateFloatAsState properly reacts to state changes unlike infinite transition
  val targetScale = if (isConnected) 1.05f else 1f
  val scale =
    animateFloatAsState(
      targetValue = targetScale,
      animationSpec = spring(dampingRatio = 0.6f),
      label = "scale",
    ).value

  val borderColor = if (isConnected) AppTheme.colors.successPrimary else AppTheme.colors.accentPrimary
  val bgColor = if (isConnected) AppTheme.colors.successPrimary.copy(alpha = 0.15f) else AppTheme.colors.backgroundSecondary

  Box(
    modifier =
      Modifier
        .size(160.dp)
        .scale(scale)
        .border(BorderStroke(2.dp, borderColor), CircleShape)
        .background(bgColor, CircleShape)
        .clickable(enabled = !isConnecting) { onClick() },
    contentAlignment = Alignment.Center,
  ) {
    if (isConnecting) {
      CircularProgressIndicator(color = borderColor, modifier = Modifier.size(48.dp))
    } else {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
          painter = painterResource(id = R.drawable.ic_home_24),
          contentDescription = null,
          tint = borderColor,
          modifier = Modifier.size(48.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = if (isConnected) stringResource(R.string.home_btn_disconnect) else stringResource(R.string.home_btn_connect),
          style = AppTheme.typography.labelLarge,
          color = borderColor,
        )
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenDisconnectedPreview() {
  AppTheme {
    ConnectButton(state = ScreenState()) {}
  }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenConnectedPreview() {
  AppTheme {
    ConnectButton(
      state =
        ScreenState(
          workState = WorkState.Running,
          connectedProfile = ConnectionProfile(protocol = Protocol.Vmess, subscriptionId = "test", remarks = "Test Server"),
        ),
    ) {}
  }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenConnectingPreview() {
  AppTheme {
    ConnectButton(state = ScreenState(workState = WorkState.Running)) {}
  }
}
