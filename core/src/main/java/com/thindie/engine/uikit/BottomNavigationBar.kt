package com.thindie.engine.uikit

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp

@Composable
fun BottomNavigationBar(
  modifier: Modifier = Modifier,
  items: List<BottomNavItem>,
  selected: Int,
  onItemClicked: (Int) -> Unit,
) {
  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .heightIn(min = 64.dp),
    horizontalArrangement = Arrangement.SpaceEvenly,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    items.forEachIndexed { index, item ->
      val color =
        animateColorAsState(
          targetValue =
            if (selected == index) AppTheme.colors.accentPrimary else AppTheme.colors.contentSecondary,
          label = "bottom-nav-color",
        ).value
      Column(
        modifier =
          Modifier
            .weight(1f)
            .clickable(onClick = { onItemClicked(index) }, interactionSource = null, indication = null),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Icon(painter = item.icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        VSpacer(4.dp)
        Text(text = item.title, style = AppTheme.typography.labelMedium, color = color, maxLines = 1)
      }
    }
  }
}
@Immutable
data class BottomNavItem(val icon: Painter, val title: String)
