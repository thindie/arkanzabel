package com.thindie.rknzbl.appfeatures.settings.ui.perapp

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.thindie.engine.core.ScreenScope
import com.thindie.engine.uikit.Action
import com.thindie.engine.uikit.AppScreen
import com.thindie.engine.uikit.AppTheme
import com.thindie.engine.uikit.HelpSecondaryText
import com.thindie.engine.uikit.HelpSectionHeading
import com.thindie.engine.uikit.SentenceRow
import com.thindie.engine.uikit.surface
import com.thindie.rknzbl.R
import com.thindie.rknzbl.appfeatures.settings.ui.searchapppackage.rememberAppIconPainter

@Composable
internal fun PerAppProxyScreen(scope: ScreenScope<PerAppViewState, PerAppCommand>) {
  val screenState by scope.state.collectAsState()
  AppScreen(
    screenScope = scope,
    modifier = Modifier.imePadding(),
    title = null,
    primary =
      Action(
        resRef = R.drawable.ic_arrow_back_24,
        listener = { scope.send(PerAppCommand.Back) },
      ),
  ) {
    BackHandler { scope.send(PerAppCommand.Back) }
    val appsByPackage = screenState.allApps.associateBy { it.packageName }
    LazyColumn(
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      stickyHeader {
        Row(
          modifier =
            Modifier
              .fillMaxWidth()
              .background(AppTheme.colors.backgroundPrimary),
        ) {
          Column {
            HelpSectionHeading(stringResource(R.string.per_app_proxy_title))
            HelpSecondaryText(stringResource(R.string.per_app_proxy_mode_section))
          }
        }
      }
      item {
        SentenceRow(
          modifier =
            Modifier
              .border(
                border =
                  BorderStroke(
                    width = 1.2.dp,
                    color =
                      if (screenState.mode == ProxyScopeMode.All) {
                        AppTheme.colors.accentPrimary
                      } else {
                        AppTheme.colors.backgroundSecondary
                      },
                  ),
                shape = RoundedCornerShape(20.dp),
              )
              .fillMaxWidth(),
          painter = painterResource(R.drawable.ic_internet_24),
          title = stringResource(R.string.per_app_proxy_mode_all),
          subtitle = stringResource(R.string.per_app_proxy_mode_all_subtitle),
          loading = false,
          onClick = { scope.send(PerAppCommand.SetModeAll) },
        )
      }
      item {
        SentenceRow(
          modifier =
            Modifier
              .border(
                border =
                  BorderStroke(
                    width = 1.2.dp,
                    color =
                      if (screenState.mode == ProxyScopeMode.Selected) {
                        AppTheme.colors.accentPrimary
                      } else {
                        AppTheme.colors.backgroundSecondary
                      },
                  ),
                shape = RoundedCornerShape(20.dp),
              )
              .fillMaxWidth(),
          painter = painterResource(R.drawable.ic_information_24),
          title = stringResource(R.string.per_app_proxy_mode_selected),
          subtitle = stringResource(R.string.per_app_proxy_mode_selected_subtitle),
          loading = false,
          onClick = { scope.send(PerAppCommand.SetModeSelected) },
        )
      }
      if (screenState.mode == ProxyScopeMode.Selected) {
        stickyHeader {
          Row(
            modifier =
              Modifier
                .fillMaxWidth()
                .background(AppTheme.colors.backgroundPrimary),
          ) {
            Column {
              HelpSectionHeading(stringResource(R.string.per_app_proxy_applied_section))
              val description =
                if (screenState.selectedPackages.isEmpty()) {
                  stringResource(R.string.per_app_proxy_none_selected)
                } else {
                  stringResource(R.string.per_app_proxy_mode_selected_header_subtitle)
                }
              HelpSecondaryText(description)
            }
          }
        }
        items(
          items = screenState.selectedPackages.sorted(),
          key = { it },
        ) { pkg ->
          val row = appsByPackage[pkg]
          val appIcon = rememberAppIconPainter(pkg)
          SentenceRow(
            modifier =
              Modifier
                .border(
                  border = BorderStroke(1.2.dp, AppTheme.colors.backgroundSecondary),
                  shape = RoundedCornerShape(20.dp),
                )
                .fillMaxWidth(),
            painter = appIcon ?: painterResource(R.drawable.ic_internet_24),
            tintIcon = appIcon == null,
            title = row?.appName ?: pkg,
            subtitle = row?.packageName ?: pkg,
            loading = false,
            onClick = { scope.send(PerAppCommand.RemovePackage(pkg)) },
          )
        }
        stickyHeader {
          Row(
            modifier =
              Modifier
                .fillMaxWidth()
                .background(AppTheme.colors.backgroundPrimary),
          ) {
            Column {
              HelpSectionHeading(stringResource(R.string.per_app_proxy_open_search_title))
              HelpSecondaryText(stringResource(R.string.per_app_proxy_open_search_subtitle))
            }
          }
        }
        item {
          Box(
            modifier =
              Modifier
                .fillMaxWidth()
                .height(52.dp)
                .surface(
                  shape = RoundedCornerShape(16.dp),
                  onClick = { scope.send(PerAppCommand.OpenSearch) },
                  backgroundColor = AppTheme.colors.backgroundSecondary,
                ),
          )
        }
      }
    }
  }
}
