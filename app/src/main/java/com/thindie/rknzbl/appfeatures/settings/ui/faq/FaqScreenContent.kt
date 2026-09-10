package com.thindie.rknzbl.appfeatures.settings.ui.faq

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import com.thindie.engine.uikit.HelpParagraph
import com.thindie.engine.uikit.HelpSecondaryText
import com.thindie.engine.uikit.HelpSectionHeading
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
      HelpSectionHeading(stringResource(R.string.faq_storage_title))
      VSpacer(8.dp)
      HelpParagraph(stringResource(R.string.faq_storage_body_1))
      VSpacer(8.dp)
      HelpParagraph(stringResource(R.string.faq_storage_body_2))
      VSpacer(8.dp)
      HelpSecondaryText(stringResource(R.string.faq_storage_diagram_remote))
      VSpacer(4.dp)
      HelpSecondaryText(stringResource(R.string.faq_storage_diagram_local))

      // === Per-app proxy ===
      VSpacer(24.dp)
      Divider()
      VSpacer(16.dp)
      HelpSectionHeading(stringResource(R.string.faq_perapp_title))
      VSpacer(8.dp)
      HelpParagraph(stringResource(R.string.faq_perapp_body_1))
      VSpacer(8.dp)
      HelpSecondaryText(stringResource(R.string.faq_perapp_steps))

      // === Notification legend ===
      VSpacer(24.dp)
      Divider()
      VSpacer(16.dp)
      HelpSectionHeading(stringResource(R.string.faq_legend_title))
      VSpacer(8.dp)
      HelpParagraph(stringResource(R.string.faq_legend_body))
      VSpacer(8.dp)
      FaqLegendItem(iconRes = EngineR.drawable.ic_stat_proxy, label = stringResource(R.string.faq_legend_proxy))
      VSpacer(4.dp)
      FaqLegendItem(iconRes = EngineR.drawable.ic_stat_direct, label = stringResource(R.string.faq_legend_direct))
      VSpacer(4.dp)
      FaqLegendItem(iconRes = EngineR.drawable.ic_stat_name, label = stringResource(R.string.faq_legend_idle))
      VSpacer(8.dp)
      HelpParagraph(stringResource(R.string.faq_legend_format))

      // === Logs ===
      VSpacer(24.dp)
      Divider()
      VSpacer(16.dp)
      HelpSectionHeading(stringResource(R.string.faq_logs_title))
      VSpacer(8.dp)
      HelpParagraph(stringResource(R.string.faq_logs_body))

      // === Updates ===
      VSpacer(24.dp)
      Divider()
      VSpacer(16.dp)
      HelpSectionHeading(stringResource(R.string.faq_updates_title))
      VSpacer(8.dp)
      HelpParagraph(stringResource(R.string.faq_updates_body))
      FaqRepoLink(REPO_URL)

      VSpacer(32.dp)
    }
  }
}

private const val REPO_URL = "https://github.com/thindie/arkanzabel"

@Composable
private fun FaqRepoLink(url: String) {
  val activity = LocalActivity.current
  Text(
    text = url,
    modifier =
      Modifier
        .fillMaxWidth()
        .clickable {
          activity?.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        },
    style = AppTheme.typography.bodyMedium,
    color = AppTheme.colors.accentPrimary,
  )
}

@Preview(showBackground = true)
@Composable
private fun FaqRepoLinkPreview() {
  AppTheme {
    FaqRepoLink(REPO_URL)
  }
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
private fun FaqLegendItemPreview() {
  AppTheme {
    FaqLegendItem(
      iconRes = EngineR.drawable.ic_stat_proxy,
      label = "Up arrow — traffic goes through the VPN",
    )
  }
}
