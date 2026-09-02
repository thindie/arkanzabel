package com.thindie.rknzbl.appfeatures.home.ui

import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.thindie.engine.core.ScreenScope
import com.thindie.engine.uikit.AppTheme

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
      if (state.isConnected) {
        Text(
          text = "Connected",
          style = AppTheme.typography.labelMedium,
          color = Color(0xFF4CAF50),
        )
        if (state.serverName.isNotEmpty()) {
          Text(
            text = state.serverName,
            style = AppTheme.typography.bodySmall,
            color = AppTheme.colors.contentSecondary,
          )
        }
      }

      // Big pulsing button
      ConnectButton(isConnected = state.isConnected)

      // Hint below
      if (!state.isConnected) {
        Text(
          text = "Tap to connect",
          style = AppTheme.typography.labelMedium,
          color = AppTheme.colors.contentSecondary,
        )
      }
    }
  }
}

@Composable
private fun ConnectButton(isConnected: Boolean) {
  val transition = rememberInfiniteTransition(label = "pulse")
  val scale = transition.animateFloat(
    initialValue = 1f,
    targetValue = if (isConnected) 1.05f else 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 800),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "scale",
  ).value

  val borderColor = if (isConnected) Color(0xFF4CAF50) else AppTheme.colors.accentPrimary
  val bgColor = if (isConnected) Color(0xFF1B3A2A) else AppTheme.colors.backgroundSecondary

  Box(
    modifier = Modifier
      .size(160.dp)
      .scale(scale)
      .border(BorderStroke(2.dp, borderColor), CircleShape)
      .background(bgColor, CircleShape)
      .clickable { /* TODO: trigger connect/disconnect */ },
    contentAlignment = Alignment.Center,
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Icon(
        painter = painterResource(id = com.thindie.rknzbl.R.drawable.ic_home_24),
        contentDescription = null,
        tint = borderColor,
        modifier = Modifier.size(48.dp),
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = if (isConnected) "DISCONNECT" else "CONNECT",
        style = AppTheme.typography.labelLarge,
        color = borderColor,
      )
    }
  }
}
