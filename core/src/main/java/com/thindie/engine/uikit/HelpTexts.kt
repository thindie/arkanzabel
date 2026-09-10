package com.thindie.engine.uikit

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun HelpSectionHeading(text: String) {
  Text(
    text = text,
    style = AppTheme.typography.titleMedium,
    color = AppTheme.colors.contentSecondary,
  )
}

@Preview(showBackground = true)
@Composable
private fun HelpSectionHeadingPreview() {
  AppTheme {
    HelpSectionHeading("Where are my profiles stored?")
  }
}

@Composable
fun HelpParagraph(text: String) {
  Text(
    text = text,
    modifier = Modifier.fillMaxWidth(),
    style = AppTheme.typography.bodyMedium,
    color = AppTheme.colors.contentPrimary,
  )
}

@Preview(showBackground = true)
@Composable
private fun HelpParagraphPreview() {
  AppTheme {
    HelpParagraph("By default, Arkanzabel syncs your connection profiles over WebDAV.")
  }
}

@Composable
fun HelpSecondaryText(text: String) {
  Text(
    text = text,
    modifier = Modifier.fillMaxWidth(),
    style = AppTheme.typography.bodySmall,
    color = AppTheme.colors.contentSecondary,
  )
}

@Preview(showBackground = true)
@Composable
private fun HelpSecondaryTextPreview() {
  AppTheme {
    HelpSecondaryText("Remote: Profiles -> WebDAV server -> shared pool")
  }
}
