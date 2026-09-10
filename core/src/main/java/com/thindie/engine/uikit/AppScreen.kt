package com.thindie.engine.uikit

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.thindie.engine.core.Command
import com.thindie.engine.core.ScreenScope
import com.thindie.engine.core.ServiceCommand
import com.thindie.engine.core.ViewState
import kotlinx.coroutines.delay

@Composable
fun <S : ViewState, C : Command> AppScreen(
  screenScope: ScreenScope<S, C>,
  modifier: Modifier = Modifier,
  title: String? = null,
  subtitle: String? = null,
  primary: Action? = null,
  secondary: Action? = null,
  content: @Composable () -> Unit,
) {
  AnimatedContent(
    modifier =
      modifier
        .background(AppTheme.colors.backgroundPrimary),
    targetState = screenScope,
  ) { _ ->
    if (screenScope.error.value != null) {
      screenScope.ErrorMessage()
    } else {
      Box(
        Modifier
          .fillMaxSize()
          .background(AppTheme.colors.backgroundPrimary),
      ) {
        Column(
          modifier = Modifier.systemBarsPadding(),
        ) {
          TopAppBar(
            title = title,
            description = subtitle,
            primary = primary,
            secondary = secondary,
          )
          content()
        }
        var showEvent by remember { mutableStateOf<ServiceCommand.UiEvent?>(null) }
        LaunchedEffect(screenScope) {
          screenScope.event
            .collect {
              showEvent = it
            }
        }
        if (showEvent != null) {
          when (showEvent) {
            is ServiceCommand.UiEvent.Decision -> {
              Dialog(
                onDismiss = {
                  showEvent = null
                },
                content = {
                  (showEvent as ServiceCommand.UiEvent.Decision).content.invoke()
                },
                primary =
                  (showEvent as ServiceCommand.UiEvent.Decision).primaryAction.let {
                    it.copy(
                      listener = {
                        it.listener()
                        showEvent = null
                      },
                    )
                  },
                secondary =
                  (showEvent as ServiceCommand.UiEvent.Decision).secondaryAction?.let {
                    it.copy(
                      listener = {
                        it.listener()
                        showEvent = null
                      },
                    )
                  },
              )
            }

            is ServiceCommand.UiEvent.Snack, is ServiceCommand.UiEvent.SnackText -> {
              LaunchedEffect(showEvent) {
                delay(2000)
                showEvent = null
              }
              AnimatedVisibility(
                modifier =
                  Modifier
                    .align(Alignment.TopCenter)
                    .clickable(
                      onClick = {
                        (showEvent as? ServiceCommand.UiEvent.Snack)?.action?.listener?.invoke()
                      },
                      indication = null,
                      interactionSource = null,
                    )
                    .fillMaxWidth()
                    .padding(top = 56.dp)
                    .padding(all = 16.dp)
                    .background(AppTheme.colors.accentPrimary, shape = RoundedCornerShape(16.dp))
                    .padding(all = 16.dp),
                visible = showEvent is ServiceCommand.UiEvent.Snack || showEvent is ServiceCommand.UiEvent.SnackText,
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  val ref = (showEvent as? ServiceCommand.UiEvent.Snack)?.action?.resRef
                  val text =
                    if (ref != null) {
                      stringResource(ref)
                    } else {
                      (showEvent as ServiceCommand.UiEvent.SnackText).text
                    }
                  Text(
                    text = text,
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colors.onAccentPrimary,
                  )
                }
              }
            }

            null -> error("Must not be reached")
          }
        }
        if (screenScope.processing.value != null) {
          Box(
            Modifier
              .fillMaxSize()
              .background(
                Color.Transparent.copy(alpha = 0.3f),
              )
              .clickable(onClick = {}, enabled = false),
          ) {
            CircularProgress(
              modifier =
                Modifier
                  .align(Alignment.Center)
                  .background(
                    color = AppTheme.colors.backgroundSecondary,
                    shape = RoundedCornerShape(20.dp),
                  )
                  .padding(16.dp),
            )
          }
        }
      }
    }
  }
}
