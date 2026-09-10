package com.thindie.engine.uikit

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun Toggle(
  checked: Boolean,
  enabled: Boolean = true,
  onClick: (() -> Unit)? = null,
) {
  val startPadding by animateDpAsState(
    targetValue = if (!checked) 1.4.dp else 26.dp,
    animationSpec = tween(durationMillis = 400, easing = LinearOutSlowInEasing),
  )

  val endPadding by animateDpAsState(
    targetValue = if (checked) 1.4.dp else 26.dp,
    animationSpec = tween(durationMillis = 400, easing = LinearOutSlowInEasing),
  )

  val color by animateColorAsState(
    targetValue =
      when {
        !enabled -> {
          AppTheme.colors.backgroundSecondary
        }

        checked -> {
          AppTheme.colors.accentPrimary
        }

        else -> {
          AppTheme.colors.contentSecondary
        }
      },
    animationSpec = tween(durationMillis = 400, easing = LinearOutSlowInEasing),
  )

  val toggleModifier =
    if (onClick != null) {
      Modifier.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
      ) { onClick() }
    } else {
      Modifier
    }

  Box(
    modifier =
      toggleModifier
        .width(52.dp)
        .height(28.dp)
        .background(color, shape = CircleShape)
        .padding(start = startPadding, end = endPadding, top = 1.4.dp, bottom = 1.4.dp)
        .background(color = AppTheme.colors.backgroundPrimary, CircleShape),
  )
}
