package com.thindie.rknzbl.uikit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thindie.rknzbl.R
import com.thindie.rknzbl.engine.Command
import com.thindie.rknzbl.engine.ScreenScope
import com.thindie.rknzbl.engine.ServiceCommand
import com.thindie.rknzbl.engine.State
import com.thindie.rknzbl.engine.ref
import kotlinx.coroutines.delay

private object ContentAlpha {
  const val DISABLED: Float = 0.3f
}

enum class ProfileBorderState { Inactive, Testing, Connected, Failed }

@Composable
fun Modifier.profileBorder(state: ProfileBorderState): Modifier {
  var progress by remember { mutableStateOf(0f) }
  var moveRight by remember { mutableStateOf(true) }
  LaunchedEffect(state) {
    if (state != ProfileBorderState.Testing) {
      progress = 0f
      return@LaunchedEffect
    }
    while (true) {
      if (moveRight) {
        if (progress > 1f) {
          moveRight = false
          progress -= 0.1f
        } else {
          progress += 0.1f
        }
      } else {
        if (progress < -1f) {
          moveRight = true
          progress += 0.1f
        } else {
          progress -= 0.1f
        }
      }
      delay(50)
    }
  }
  val colors = AppTheme.colors
  return when (state) {
    ProfileBorderState.Inactive ->
      border(
        border = BorderStroke(1.2.dp, colors.backgroundSecondary),
        shape = RoundedCornerShape(20.dp),
      )
    ProfileBorderState.Testing ->
      border(
        brush =
          Brush.linearGradient(
            colors =
              listOf(
                colors.contentPrimary,
                colors.contentSecondary,
                colors.backgroundSecondary,
                colors.contentSecondary,
                colors.backgroundPrimary,
              ),
            start = Offset(progress * 500f, 0f),
            end = Offset((progress + 1f) * 500f, 200f),
          ),
        shape = RoundedCornerShape(20.dp),
        width = 1.2.dp,
      )
    ProfileBorderState.Connected ->
      border(
        border = BorderStroke(1.2.dp, colors.accentPrimary),
        shape = RoundedCornerShape(20.dp),
      )
    ProfileBorderState.Failed ->
      border(
        border = BorderStroke(1.2.dp, colors.errorPrimary),
        shape = RoundedCornerShape(20.dp),
      )
  }
}

@Composable
fun Button(
  modifier: Modifier = Modifier,
  text: String,
  onClick: () -> Unit,
  loading: Boolean = false,
  enabled: Boolean = true,
) {
  val contentColor by animateColorAsState(
    if (enabled) {
      AppTheme.colors.buttonContentPrimary
    } else {
      AppTheme.colors.contentSecondary
    },
  )

  val backgroundColor by animateColorAsState(
    if (enabled) {
      if (loading) {
        AppTheme.colors.onAccentPrimary
      } else {
        AppTheme.colors.accentPrimary
      }
    } else {
      AppTheme.colors.backgroundSecondary
    },
  )
  Box(
    modifier =
      modifier
        .height(52.dp)
        .widthIn(min = 328.dp)
        .surface(
          shape = RoundedCornerShape(16.dp),
          backgroundColor = backgroundColor,
          shadowElevation = 0f,
          border = null,
          enabled = enabled && !loading,
          onClick = onClick,
        )
        .fillMaxSize()
        .padding(horizontal = 24.dp),
    contentAlignment = Alignment.Center,
  ) {
    AnimatedVisibility(
      visible = loading,
      enter = fadeIn(),
      exit = fadeOut(),
    ) {
      CircularProgress(
        modifier = Modifier.size(24.dp),
      )
    }

    AnimatedVisibility(
      visible = !loading,
      enter = fadeIn(),
      exit = fadeOut(),
    ) {
      Text(
        text = text.uppercase(),
        style = AppTheme.typography.button,
        color = contentColor,
        textAlign = TextAlign.Center,
      )
    }
  }
}

@Stable
fun Modifier.surface(
  shape: Shape = RoundedCornerShape(16.dp),
  backgroundColor: Color = Color.Transparent,
  border: BorderStroke? = null,
  shadowElevation: Float = 0f,
  enabled: Boolean = true,
  onClick: (() -> Unit)? = null,
) = this
  .graphicsLayer(shadowElevation = shadowElevation, shape = shape, clip = false)
  .then(if (border != null) Modifier.border(border, shape) else Modifier)
  .background(color = backgroundColor, shape = shape)
  .clip(shape)
  .clickable(
    onClick =
      if (onClick != null && enabled) {
        onClick
      } else {
        { }
      },
    enabled = onClick != null && enabled,
  )

@Composable
fun <S : State, C : Command> ScreenScope<S, C>.ErrorMessage() {
  val error = this@ErrorMessage.error.value ?: return
  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .padding(16.dp)
        .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = error.message,
      style = AppTheme.typography.titleMedium,
    )
    Column(
      verticalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.padding(top = 16.dp),
    ) {
      error.actions.entries.forEach { (a, cmd) ->
        Button(
          text = stringResource(a.ref ?: R.string.btn_close),
          onClick = {
            when {
              cmd as? ServiceCommand.Prioritized != null -> cmd.execute()
              else -> send(cmd as C)
            }
          },
          loading = processing.value == cmd,
        )
      }
    }
  }
}

@Composable
fun SentenceRow(
  modifier: Modifier = Modifier,
  painter: Painter?,
  title: String,
  subtitle: String?,
  enabled: Boolean = true,
  loading: Boolean?,
  tintIcon: Boolean = true,
  onClick: (() -> Unit)? = null,
  onLongClick: (() -> Unit)? = null,
) {
  val colorsPrimary =
    if (enabled) {
      AppTheme.colors.backgroundPrimary
    } else {
      AppTheme.colors.backgroundPrimary.copy(alpha = ContentAlpha.DISABLED)
    }

  val tint =
    if (enabled) {
      AppTheme.colors.accentPrimary
    } else {
      AppTheme.colors.accentPrimary.copy(alpha = ContentAlpha.DISABLED)
    }
  val iconTint = if (tintIcon) tint else Color.Unspecified

  val colorsSecondary =
    if (enabled) {
      AppTheme.colors.backgroundSecondary
    } else {
      AppTheme.colors.backgroundSecondary.copy(alpha = ContentAlpha.DISABLED)
    }
  val interactionModifier =
    when {
      !enabled -> Modifier
      onLongClick != null && onClick != null ->
        Modifier.combinedClickable(
          onClick = onClick,
          onLongClick = onLongClick,
        )

      onClick != null -> Modifier.clickable(onClick = onClick)
      onLongClick != null ->
        Modifier.combinedClickable(
          onClick = { },
          onLongClick = onLongClick,
        )

      else -> Modifier
    }
  Row(
    modifier =
      modifier
        .background(color = colorsPrimary, shape = RoundedCornerShape(20.dp))
        .clip(shape = RoundedCornerShape(20.dp))
        .then(interactionModifier)
        .padding(horizontal = 16.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(contentAlignment = Alignment.Center) {
      if (loading != null) {
        Crossfade(
          modifier = Modifier.size(40.dp),
          targetState = loading,
        ) { loading ->
          if (loading) {
            CircularProgress(
              modifier =
                Modifier
                  .padding(8.dp)
                  .size(32.dp),
            )
          } else {
            if (painter == null) {
              HSpacer(40.dp)
            } else {
              Icon(
                painter = painter,
                contentDescription = null,
                modifier =
                  Modifier
                    .background(color = colorsSecondary, shape = RoundedCornerShape(20.dp))
                    .padding(8.dp)
                    .size(32.dp),
                tint = iconTint,
              )
            }
          }
        }
      } else {
        if (painter == null) {
          HSpacer(40.dp)
        } else {
          Icon(
            painter = painter,
            contentDescription = null,
            modifier =
              Modifier
                .background(color = colorsSecondary, shape = RoundedCornerShape(20.dp))
                .padding(8.dp)
                .size(32.dp),
            tint = iconTint,
          )
        }
      }
    }
    HSpacer(12.dp)
    if (subtitle != null) {
      Column {
        Text(
          text = title,
          style = AppTheme.typography.titleMedium,
          color = AppTheme.colors.contentPrimary,
        )
        VSpacer(2.dp)
        Text(
          text = subtitle,
          style = AppTheme.typography.bodyMedium,
          color = AppTheme.colors.contentSecondary,
        )
      }
    } else {
      Text(
        text = title,
        style = AppTheme.typography.titleMedium,
        color = AppTheme.colors.contentPrimary,
      )
    }
  }
}

@Composable
fun CircularProgress(modifier: Modifier = Modifier) {
  CircularProgressIndicator(
    modifier = modifier,
    color = AppTheme.colors.accentPrimary,
    strokeWidth = 1.2.dp,
    strokeCap = StrokeCap.Round,
  )
}

@Immutable
data class Action(
  val listener: () -> Unit,
  val resRef: Int,
)

@Composable
fun TopAppBar(
  title: String? = null,
  description: String? = null,
  primary: Action? = null,
  secondary: Action? = null,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (primary != null) {
      IconButton(onClick = primary.listener) {
        Icon(
          painter = painterResource(primary.resRef),
          contentDescription = null,
          tint = AppTheme.colors.accentPrimary,
        )
      }
    } else {
      HSpacer(12.dp)
    }
    if (description != null) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
          text = title.orEmpty(),
          style = AppTheme.typography.titleLarge,
          color = AppTheme.colors.contentSecondary,
        )
        VSpacer(2.dp)
        Text(
          text = description,
          style = AppTheme.typography.labelLarge,
          color = AppTheme.colors.accentPrimary,
        )
      }
    } else {
      Text(
        text = title.orEmpty(),
        style = AppTheme.typography.titleLarge,
        color = AppTheme.colors.contentSecondary,
      )
    }
    if (secondary != null) {
      IconButton(onClick = secondary.listener) {
        Icon(
          painter = painterResource(secondary.resRef),
          contentDescription = null,
          tint = AppTheme.colors.accentPrimary,
        )
      }
    } else {
      HSpacer(12.dp)
    }
  }
}

@Composable
fun Dialog(
  content: @Composable () -> Unit,
  onDismiss: () -> Unit,
  primary: Action,
  secondary: Action? = null,
) {
  AlertDialog(
    containerColor = AppTheme.colors.backgroundSecondary,
    onDismissRequest = onDismiss,
    text = {
      content()
    },
    confirmButton = {
      Button(
        text = stringResource(primary.resRef),
        onClick = {
          primary.listener.invoke()
        },
      )
    },
    dismissButton =
      if (secondary != null) {
        {
          Button(
            text = stringResource(secondary.resRef),
            onClick = {
              secondary.listener()
            },
          )
        }
      } else {
        null
      },
  )
}
