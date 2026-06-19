package com.thindie.rknzbl.uikit

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Custom animated toggle switch with smooth transitions.
 * Provides a modern, polished appearance with color and size animations.
 */
@Composable
fun Toggle(checked: Boolean) {
  val startPadding by animateDpAsState(
    targetValue = if (!checked) 1.4.dp else 26.dp,
    animationSpec = tween(durationMillis = 400, easing = LinearOutSlowInEasing)
  )

  val endPadding by animateDpAsState(
    targetValue = if (checked) 1.4.dp else 26.dp,
    animationSpec = tween(durationMillis = 400, easing = LinearOutSlowInEasing)
  )

  val color by animateColorAsState(
    targetValue = if (checked) {
      AppTheme.colors.accentPrimary
    } else {
      AppTheme.colors.backgroundSecondary
    },
    animationSpec = tween(durationMillis = 400, easing = LinearOutSlowInEasing)
  )

  Box(
    modifier = Modifier
      .width(52.dp)
      .height(28.dp)
      .background(color, shape = CircleShape)
      .padding(start = startPadding, end = endPadding, top = 1.4.dp, bottom = 1.4.dp)
      .background(color = AppTheme.colors.backgroundPrimary)
  )
}
