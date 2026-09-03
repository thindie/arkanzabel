package com.thindie.engine.uikit

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
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
) {
  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .background(AppTheme.colors.backgroundSecondary, shape = RoundedCornerShape(12.dp))
        .padding(4.dp),
  ) {
    items.forEachIndexed { index, item ->
      val isSelected = index == selected
      val backgroundColor = animateColorAsState(
        targetValue = if (isSelected) AppTheme.colors.accentPrimary else Color.Transparent,
        label = "tab-bg",
      ).value
      Box(
        modifier =
          Modifier
            .weight(1f)
            .background(color = backgroundColor, shape = RoundedCornerShape(8.dp))
            .clickable { onTabSelected(index) }
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
}

data class TabItem(val label: String)

@Preview(showBackground = true)
@Composable
private fun TabRowPreview() {
  AppTheme {
    TabRow(
      items = listOf(TabItem("Main"), TabItem("Saved")),
      selected = 0,
      onTabSelected = {},
    )
  }
}
