package com.thindie.engine.uikit

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

/**
 * Segmented tab row for switching between top-level sections of a screen.
 */
@Composable
fun TabRow(
  modifier: Modifier = Modifier,
  items: List<TabItem>,
  selected: Int,
  onTabSelected: (Int) -> Unit,
  content: @Composable (Int) -> Unit,
) {
  Column(modifier = modifier.fillMaxWidth()) {
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .background(AppTheme.colors.backgroundSecondary, shape = RoundedCornerShape(12.dp))
          .padding(4.dp),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      items.forEachIndexed { index, item ->
        val isSelected = index == selected
        val backgroundColor =
          animateColorAsState(
            targetValue = if (isSelected) AppTheme.colors.accentPrimary else Color.Transparent,
            label = "tab-bg",
          ).value
        Box(
          modifier =
            Modifier
              .weight(1f)
              .background(color = backgroundColor, shape = RoundedCornerShape(8.dp))
              .clickable(
                indication = null,
                interactionSource = null,
              ) { onTabSelected(index) }
              .padding(vertical = 8.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = item.label,
            style = AppTheme.typography.titleSmall,
            color = if (isSelected) AppTheme.colors.onAccentPrimary else AppTheme.colors.contentSecondary,
          )
        }
      }
    }

    AnimatedContent(
      modifier = Modifier.weight(1f),
      targetState = selected,
      transitionSpec = {
        val tween = tween<IntOffset>(durationMillis = 250)
        if (targetState > initialState) {
          slideInHorizontally(tween) { it } + fadeIn(tween()) togetherWith
            slideOutHorizontally(tween) { -it } + fadeOut(tween())
        } else {
          slideInHorizontally(tween) { -it } + fadeIn(tween()) togetherWith
            slideOutHorizontally(tween) { it } + fadeOut(tween())
        }
      },
    ) { index ->
      content(index)
    }
  }
}

@Immutable
data class TabItem(val label: String)

@Preview(showBackground = true)
@Composable
private fun TabRowPreview() {
  AppTheme {
    var selected by remember { mutableStateOf(0) }
    TabRow(
      items = listOf(TabItem("Main"), TabItem("Saved")),
      selected = selected,
      onTabSelected = { selected = it },
      content = { index ->
        Box(
          modifier = Modifier.fillMaxSize().padding(16.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text("Content for tab ${index + 1}")
        }
      },
    )
  }
}
