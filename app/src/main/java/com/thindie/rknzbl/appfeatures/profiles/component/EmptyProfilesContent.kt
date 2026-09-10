package com.thindie.rknzbl.appfeatures.profiles.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.thindie.engine.uikit.AppTheme
import com.thindie.engine.uikit.VSpacer
import com.thindie.rknzbl.R

@Composable
internal fun EmptyProfilesContent(
  icon: Int,
  title: String,
  subtitle: String? = null,
) {
  Column(
    modifier =
      Modifier
        .fillMaxWidth()
        .fillMaxHeight()
        .padding(horizontal = 24.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Icon(
      painter = painterResource(icon),
      contentDescription = null,
      modifier = Modifier.size(72.dp),
      tint = AppTheme.colors.contentSecondary,
    )
    VSpacer(16.dp)
    Text(
      text = title,
      style = AppTheme.typography.titleLarge,
      color = AppTheme.colors.contentPrimary,
      textAlign = TextAlign.Center,
    )
    subtitle?.let { sub ->
      VSpacer(6.dp)
      Text(
        text = sub,
        style = AppTheme.typography.bodyMedium,
        color = AppTheme.colors.contentSecondary,
        textAlign = TextAlign.Center,
      )
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun EmptyProfilesContentPreview() {
  AppTheme {
    EmptyProfilesContent(
      icon = R.drawable.ic_globus_24,
      title = stringResource(R.string.profiles_empty),
      subtitle = stringResource(R.string.home_select_new_profiles_subtitle),
    )
  }
}
