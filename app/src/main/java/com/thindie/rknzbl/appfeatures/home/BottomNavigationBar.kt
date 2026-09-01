package com.thindie.rknzbl.appfeatures.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thindie.engine.uikit.AppTheme
import com.thindie.engine.uikit.VSpacer

/**
 * Bottom navigation bar for the new home design.
 *
 * Two tabs — [HomeSection.Home] and [HomeSection.Settings]. Mirrors the Checkraise
 * `BottomNavigationBar`; arkanzabel uses two tabs instead of three.
 */
@Composable
fun BottomNavigationBar(
  modifier: Modifier = Modifier,
  onHomeClick: () -> Unit,
  onSettingsClick: () -> Unit,
  selected: HomeSection,
) {
  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .wrapContentHeight()
        .padding(horizontal = 24.dp)
        .padding(bottom = 16.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceEvenly,
  ) {
    Section(
      title = "Home",
      icon = painterResource(id = com.thindie.rknzbl.R.drawable.ic_home_24),
      onClick = onHomeClick,
      isSelected = selected == HomeSection.Home,
    )
    Section(
      title = "Settings",
      icon = painterResource(id = com.thindie.rknzbl.R.drawable.ic_settings_24),
      onClick = onSettingsClick,
      isSelected = selected == HomeSection.Settings,
    )
  }
}

@Composable
private fun Section(
  title: String,
  icon: Painter,
  onClick: () -> Unit,
  isSelected: Boolean,
) {
  val contentColor =
    if (isSelected) AppTheme.colors.accentPrimary else AppTheme.colors.contentSecondary
  Column(
    modifier = Modifier.clickable(onClick = onClick),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Icon(
      painter = icon,
      contentDescription = title,
      modifier = Modifier.size(40.dp),
      tint = contentColor,
    )
    VSpacer(8.dp)
    Text(
      text = title,
      style = AppTheme.typography.labelMedium,
      color = contentColor,
      textAlign = TextAlign.Center,
      modifier = Modifier.padding(horizontal = 8.dp),
    )
  }
}
