package com.thindie.rknzbl.appfeatures.home.ui

import android.content.res.Configuration
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalConfiguration
import com.thindie.engine.uikit.LocalThemeSwitcher
import com.thindie.engine.uikit.ThemeSwitcher.Choice
import kotlin.math.PI
import kotlin.math.sin

/**
 * Subtle animated background. Only visible when a profile is connected.
 * Dark theme: drifting starfield with nebula blobs.
 * Light theme: gentle water ripple circles expanding outward.
 */
@Composable
internal fun NebulaBackground(modifier: Modifier = Modifier) {
  val theme = LocalThemeSwitcher.current.themeFlow.collectAsState(Choice.Auto).value
  if (when
      (theme) {
      Choice.Dark -> true
      Choice.Light -> false
      Choice.Auto -> {
        isSystemInDarkTheme()
      }
    }
  ) {
    DarkNebulaBackground(modifier)
  } else {
    LightCloudBackground(modifier)
  }
}

@Composable
@ReadOnlyComposable
private fun isSystemInDarkTheme(): Boolean {
  val uiMode = LocalConfiguration.current.uiMode
  return (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
}

@Composable
private fun DarkNebulaBackground(modifier: Modifier) {
  val transition = rememberInfiniteTransition(label = "nebula")

  // Slow drift for the entire field (simulates camera panning through space)
  val driftX by transition.animateFloat(
    initialValue = 0f,
    targetValue = 120f,
    animationSpec =
      infiniteRepeatable(
        animation = androidx.compose.animation.core.tween(60_000, easing = LinearEasing),
        repeatMode = RepeatMode.Reverse,
      ),
    label = "driftX",
  )
  val driftY by transition.animateFloat(
    initialValue = 0f,
    targetValue = 80f,
    animationSpec =
      infiniteRepeatable(
        animation = androidx.compose.animation.core.tween(45_000, easing = LinearEasing),
        repeatMode = RepeatMode.Reverse,
      ),
    label = "driftY",
  )

  // Twinkle phase offset for stars (each star twinkles at a different rate)
  val twinklePhase by transition.animateFloat(
    initialValue = 0f,
    targetValue = PI.toFloat() * 2f,
    animationSpec =
      infiniteRepeatable(
        animation = androidx.compose.animation.core.tween(8_000, easing = LinearEasing),
        repeatMode = RepeatMode.Restart,
      ),
    label = "twinkle",
  )

  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height

    // Draw soft nebula blobs (large, blurred radial gradients)
    drawNebulaBlob(Offset(w * 0.2f + driftX * 0.3f, h * 0.3f - driftY * 0.2f), w * 0.4f, Color(0x1A9DADFF))
    drawNebulaBlob(Offset(w * 0.8f - driftX * 0.2f, h * 0.7f + driftY * 0.3f), w * 0.35f, Color(0x146B5FFF))
    drawNebulaBlob(Offset(w * 0.5f + driftX * 0.1f, h * 0.2f - driftY * 0.4f), w * 0.3f, Color(0x10FF9DAD))

    // Draw stars with twinkle
    val starCount = 60
    for (i in 0 until starCount) {
      // Deterministic pseudo-random positions based on index
      val seed = i * 7 + 3
      val baseX = ((seed * 13) % 1000) / 1000f * w
      val baseY = ((seed * 17) % 1000) / 1000f * h
      val starSize = 1.5f + ((seed * 3) % 20) / 10f // 1.5 - 3.5 dp

      // Apply drift with parallax (smaller stars move less)
      val parallax = starSize / 4f
      val x = baseX + driftX * parallax
      val y = baseY + driftY * parallax

      // Twinkle: opacity oscillates based on phase and star index
      val twinkleOffset = (i * 0.7f) % PI.toFloat()
      val alpha = 0.3f + 0.5f * (sin(twinklePhase + twinkleOffset) * 0.5f + 0.5f)

      drawCircle(
        color = Color(0xFFF5F7FA).copy(alpha = alpha),
        radius = starSize,
        center = Offset(x, y),
      )
    }
  }
}

private fun DrawScope.drawNebulaBlob(
  center: Offset,
  radius: Float,
  color: Color,
) {
  drawCircle(
    brush =
      Brush.radialGradient(
        colors = listOf(color, Color.Transparent),
        center = center,
        radius = radius,
      ),
    radius = radius,
    center = center,
  )
}

/** Light theme: elegant drifting clouds — soft, quiet, like a calm sky. */
@Composable
private fun LightCloudBackground(modifier: Modifier) {
  val transition = rememberInfiniteTransition(label = "clouds")

  // Very slow horizontal drift with gentle vertical bobbing
  val driftX by transition.animateFloat(
    initialValue = 0f,
    targetValue = 180f,
    animationSpec =
      infiniteRepeatable(
        animation = androidx.compose.animation.core.tween(120_000, easing = LinearEasing),
        repeatMode = RepeatMode.Reverse,
      ),
    label = "driftX",
  )
  val bobY by transition.animateFloat(
    initialValue = 0f,
    targetValue = 30f,
    animationSpec =
      infiniteRepeatable(
        animation = androidx.compose.animation.core.tween(80_000, easing = LinearEasing),
        repeatMode = RepeatMode.Reverse,
      ),
    label = "bobY",
  )

  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height

    // Cloud 1: large, high (top-left area)
    drawCloud(
      center = Offset(w * 0.2f + driftX * 0.25f, h * 0.2f - bobY * 0.3f),
      radius = w * 0.38f,
      color = Color(0x409DBFE8),
    )

    // Cloud 2: medium (right side)
    drawCloud(
      center = Offset(w * 0.75f - driftX * 0.15f, h * 0.45f + bobY * 0.2f),
      radius = w * 0.32f,
      color = Color(0x48A8CCE8),
    )

    // Cloud 3: smaller (bottom-center)
    drawCloud(
      center = Offset(w * 0.45f + driftX * 0.35f, h * 0.75f - bobY * 0.4f),
      radius = w * 0.28f,
      color = Color(0x50B4D8F0),
    )

    // Cloud 4: accent (top-right)
    drawCloud(
      center = Offset(w * 0.9f - driftX * 0.3f, h * 0.15f + bobY * 0.5f),
      radius = w * 0.22f,
      color = Color(0x44BCE0F8),
    )

    // Cloud 5: foreground touch (bottom-left)
    drawCloud(
      center = Offset(w * 0.1f + driftX * 0.4f, h * 0.9f - bobY * 0.2f),
      radius = w * 0.25f,
      color = Color(0x4CC8ECFF),
    )
  }
}

private fun DrawScope.drawCloud(
  center: Offset,
  radius: Float,
  color: Color,
) {
  // Soft radial gradient: colored core fading to transparent edge
  drawCircle(
    brush =
      Brush.radialGradient(
        colors = listOf(color, Color.Transparent),
        center = center,
        radius = radius,
      ),
    radius = radius,
    center = center,
  )
}
