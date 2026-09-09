package com.thindie.rknzbl.appfeatures.settings.ui.licenses

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.thindie.engine.core.ScreenScope
import com.thindie.engine.uikit.Action
import com.thindie.engine.uikit.AppScreen
import com.thindie.engine.uikit.AppTheme
import com.thindie.engine.uikit.Button
import com.thindie.engine.uikit.TopAppBar
import com.thindie.engine.uikit.VSpacer
import com.thindie.rknzbl.R
import java.io.File

@Composable
internal fun LicensesScreenContent(scope: ScreenScope<LicensesState, LicensesCommand>) {
  AppScreen(scope) {
    BackHandler { scope.send(LicensesCommand.Back) }

    TopAppBar(
      primary =
        Action(
          listener = { scope.send(LicensesCommand.Back) },
          resRef = R.drawable.ic_arrow_back_24,
        ),
    )

    val context = LocalContext.current

    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(16.dp),
    ) {
      Text(
        text = stringResource(R.string.licenses_screen_title),
        style = AppTheme.typography.headlineLarge,
        color = AppTheme.colors.contentPrimary,
      )

      // === License ===
      VSpacer(24.dp)
      Divider()
      VSpacer(16.dp)
      SectionTitle(stringResource(R.string.licenses_section_license))
      VSpacer(8.dp)
      LicenseParagraph(stringResource(R.string.licenses_notice_warranty))

      // === Source ===
      VSpacer(24.dp)
      Divider()
      VSpacer(16.dp)
      SectionTitle(stringResource(R.string.licenses_section_source))
      VSpacer(8.dp)
      LicenseParagraph(stringResource(R.string.licenses_source_repo))

      // === Full license ===
      VSpacer(24.dp)
      Divider()
      VSpacer(16.dp)
      SectionTitle(stringResource(R.string.licenses_section_full))
      VSpacer(8.dp)
      LicenseParagraph(stringResource(R.string.licenses_notice_full))

      VSpacer(16.dp)
      Row(horizontalArrangement = Arrangement.Start) {
        Button(
          text = stringResource(R.string.licenses_button_full_license),
          onClick = { openFullLicense(context) },
        )
      }

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
private fun LicenseParagraph(text: String) {
  Text(
    text = text,
    modifier = Modifier.fillMaxWidth(),
    style = AppTheme.typography.bodyMedium,
    color = AppTheme.colors.contentPrimary,
  )
}

/**
 * Copies the bundled [ASSET_LICENSES] into the app cache and opens it with a system viewer.
 *
 * FileProvider can only serve real files under its declared paths, so the asset is materialized to
 * `cacheDir` first; the resulting content URI is shared with read permission for the receiver.
 */
private fun openFullLicense(context: Context) {
  try {
    val target = File(context.cacheDir, ASSET_LICENSES)
    context.assets.open(ASSET_LICENSES).use { input ->
      target.outputStream().use { output -> input.copyTo(output) }
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.cache", target)
    context.startActivity(
      Intent(Intent.ACTION_VIEW, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
    )
  } catch (e: Exception) {
    // Asset missing or no viewer installed — nothing to surface.
  }
}

private const val ASSET_LICENSES = "open_source_licenses.html"

@Preview(showBackground = true)
@Composable
private fun LicensesScreenContentPreview() {
  AppTheme {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = stringResource(R.string.licenses_screen_title),
        style = AppTheme.typography.headlineLarge,
        color = AppTheme.colors.contentPrimary,
      )
      VSpacer(16.dp)
      SectionTitle(stringResource(R.string.licenses_section_license))
      VSpacer(8.dp)
      LicenseParagraph(stringResource(R.string.licenses_notice_warranty))
      VSpacer(24.dp)
      SectionTitle(stringResource(R.string.licenses_section_full))
      VSpacer(8.dp)
      LicenseParagraph(stringResource(R.string.licenses_notice_full))
    }
  }
}
