package com.thindie.rknzbl.feature.managegate.gatelist

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.thindie.rknzbl.R
import com.thindie.rknzbl.engine.ScreenScope
import com.thindie.rknzbl.uikit.Action
import com.thindie.rknzbl.uikit.AppScreen
import com.thindie.rknzbl.uikit.AppTheme
import com.thindie.rknzbl.uikit.Button
import com.thindie.rknzbl.uikit.SentenceRow
import com.thindie.rknzbl.uikit.VSpacer

@Composable
internal fun SelectSourceScreen(scope: ScreenScope<ScreenState, ScreenCommand>) {
  val screenState by scope.state.collectAsState()
  val context = LocalContext.current
  AppScreen(
    scope = scope,
    primary =
      Action(
        resRef = R.drawable.ic_arrow_back_24,
        listener = { scope.send(ScreenCommand.Back) },
      ),
  ) {
    Box {
      BackHandler { scope.send(ScreenCommand.Back) }
      LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        items(screenState.blackSection) { sentence ->
          val (title, description) = sentence.resolveLabels(context)
          SentenceRow(
            modifier =
              Modifier
                .border(
                  border =
                    BorderStroke(
                      width = 1.2.dp,
                      color =
                        if (screenState.selected == sentence) {
                          AppTheme.colors.accentPrimary
                        } else {
                          AppTheme.colors.backgroundSecondary
                        },
                    ),
                  shape = RoundedCornerShape(20.dp),
                )
                .fillMaxWidth(),
            painter = painterResource(R.drawable.ic_information_24),
            title = title,
            subtitle = description,
            loading = false,
            onClick = { scope.send(ScreenCommand.Select(sentence)) },
          )
        }
        item { VSpacer(56.dp) }
        items(screenState.whiteSection) { sentence ->
          val (title, description) = sentence.resolveLabels(context)
          SentenceRow(
            modifier =
              Modifier
                .border(
                  border =
                    BorderStroke(
                      width = 1.2.dp,
                      color =
                        if (screenState.selected == sentence) {
                          AppTheme.colors.accentPrimary
                        } else {
                          AppTheme.colors.backgroundSecondary
                        },
                    ),
                  shape = RoundedCornerShape(20.dp),
                )
                .fillMaxWidth(),
            painter = painterResource(R.drawable.ic_information_24),
            title = title,
            subtitle = description,
            loading = false,
            onClick = { scope.send(ScreenCommand.Select(sentence)) },
          )
        }
        item { VSpacer(72.dp) }
      }
      Button(
        modifier =
          Modifier
            .align(Alignment.BottomCenter)
            .padding(16.dp),
        enabled = screenState.selected != SelectSourceFlow.Result.NotSelected,
        text = stringResource(R.string.source_select_done),
        onClick = { scope.send(ScreenCommand.Back) },
      )
    }
  }
}
