package com.thindie.rknzbl.appfeatures.settings.ui.faq

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.thindie.engine.core.ScreenScope
import com.thindie.engine.uikit.Action
import com.thindie.engine.uikit.AppScreen
import com.thindie.engine.uikit.AppTheme
import com.thindie.engine.uikit.HSpacer
import com.thindie.engine.uikit.TopAppBar
import com.thindie.engine.uikit.VSpacer
import com.thindie.rknzbl.R
import com.thindie.rknzbl.v2rayengine.R as EngineR

@Composable
internal fun FaqScreenContent(scope: ScreenScope<FaqState, FaqCommand>) {
  AppScreen(scope) {
    BackHandler { scope.send(FaqCommand.Back) }
    TopAppBar(
      primary =
        Action(
          listener = { scope.send(FaqCommand.Back) },
          resRef = R.drawable.ic_arrow_back_24,
        ),
    )

    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(16.dp),
    ) {
      Text(
        text = stringResource(R.string.faq_screen_title),
        style = AppTheme.typography.headlineLarge,
        color = AppTheme.colors.contentPrimary,
      )

      // === Storage (WebDAV) ===
      VSpacer(24.dp)
      Divider()
      VSpacer(16.dp)
      SectionTitle(stringResource(R.string.faq_storage_title))
      VSpacer(8.dp)
      FaqParagraph(stringResource(R.string.faq_storage_body_1))
      VSpacer(8.dp)
      FaqParagraph(stringResource(R.string.faq_storage_body_2))
      VSpacer(8.dp)
      FaqDiagramRow(stringResource(R.string.faq_storage_diagram_remote))
      VSpacer(4.dp)
      FaqDiagramRow(stringResource(R.string.faq_storage_diagram_local))

      // === Per-app proxy ===
      VSpacer(24.dp)
      Divider()
      VSpacer(16.dp)
      SectionTitle(stringResource(R.string.faq_perapp_title))
      VSpacer(8.dp)
      FaqParagraph(stringResource(R.string.faq_perapp_body_1))
      VSpacer(8.dp)
      FaqDiagramRow(stringResource(R.string.faq_perapp_steps))

      // === Notification legend ===
      VSpacer(24.dp)
      Divider()
      VSpacer(16.dp)
      SectionTitle(stringResource(R.string.faq_legend_title))
      VSpacer(8.dp)
      FaqParagraph(stringResource(R.string.faq_legend_body))
      VSpacer(8.dp)
      FaqLegendItem(iconRes = EngineR.drawable.ic_stat_proxy, label = stringResource(R.string.faq_legend_proxy))
      VSpacer(4.dp)
      FaqLegendItem(iconRes = EngineR.drawable.ic_stat_direct, label = stringResource(R.string.faq_legend_direct))
      VSpacer(4.dp)
      FaqLegendItem(iconRes = EngineR.drawable.ic_stat_name, label = stringResource(R.string.faq_legend_idle))
      VSpacer(8.dp)
      FaqParagraph(stringResource(R.string.faq_legend_format))

      // === Logs ===
      VSpacer(24.dp)
      Divider()
      VSpacer(16.dp)
      SectionTitle(stringResource(R.string.faq_logs_title))
      VSpacer(8.dp)
      FaqParagraph(stringResource(R.string.faq_logs_body))

      // === Updates ===
      VSpacer(24.dp)
      Divider()
      VSpacer(16.dp)
      SectionTitle(stringResource(R.string.faq_updates_title))
      VSpacer(8.dp)
      FaqParagraph(stringResource(R.string.faq_updates_body))

      VSpacer(32.dp)
    }
  }
}

@Composable
private fun SectionTitle(text: String) {
  Text(
    text = text,
    style = AppTheme.typography.titleMedium,
    color = AppTheme.colors.contentSecondary,
  )
}

@Composable
private fun FaqParagraph(text: String) {
  Text(
    text = text,
    modifier = Modifier.fillMaxWidth(),
    style = AppTheme.typography.bodyMedium,
    color = AppTheme.colors.contentPrimary,
  )
}

@Composable
private fun FaqDiagramRow(text: String) {
  Text(
    text = text,
    modifier = Modifier.fillMaxWidth(),
    style = AppTheme.typography.bodySmall,
    color = AppTheme.colors.accentPrimary,
  )
}

@Composable
private fun FaqLegendItem(
  iconRes: Int,
  label: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Image(
      painter = painterResource(iconRes),
      contentDescription = null,
      colorFilter = ColorFilter.tint(AppTheme.colors.contentPrimary),
    )
    HSpacer(8.dp)
    Text(
      text = label,
      style = AppTheme.typography.bodyMedium,
      color = AppTheme.colors.contentPrimary,
    )
  }
}

@Preview(showBackground = true)
@Composable
private fun FaqSectionTitlePreview() {
  AppTheme {
    SectionTitle("Where are my profiles stored?")
  }
}

@Preview(showBackground = true)
@Composable
private fun FaqParagraphPreview() {
  AppTheme {
    FaqParagraph("By default, Arkanzabel syncs your connection profiles over WebDAV.")
  }
}

@Preview(showBackground = true)
@Composable
private fun FaqDiagramRowPreview() {
  AppTheme {
    FaqDiagramRow("Remote: Profiles → WebDAV server → shared pool")
  }
}

@Preview(showBackground = true)
@Composable
private fun FaqLegendItemPreview() {
  AppTheme {
    FaqLegendItem(
      iconRes = EngineR.drawable.ic_stat_proxy,
      label = "Up arrow — traffic goes through the VPN",
    )
  }
}
