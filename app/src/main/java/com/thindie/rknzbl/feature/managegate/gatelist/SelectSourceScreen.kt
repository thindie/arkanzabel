package com.thindie.rknzbl.feature.managegate.gatelist

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.thindie.engine.core.ScreenScope
import com.thindie.engine.uikit.Action
import com.thindie.engine.uikit.AppScreen
import com.thindie.engine.uikit.SentenceRow
import com.thindie.engine.uikit.VSpacer
import com.thindie.rknzbl.R

@Composable
internal fun SelectSourceScreen(scope: ScreenScope<ScreenState, ScreenCommand>) {
  val screenState by scope.state.collectAsState()
  val context = LocalContext.current
  AppScreen(
    screenScope = scope,
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
            modifier = Modifier.fillMaxWidth(),
            painter = painterResource(R.drawable.ic_information_24),
            title = title,
            subtitle = description,
            loading = false,
            onClick = { scope.send(ScreenCommand.Go(sentence)) },
          )
        }
        item { VSpacer(56.dp) }
        items(screenState.whiteSection) { sentence ->
          val (title, description) = sentence.resolveLabels(context)
          SentenceRow(
            modifier = Modifier.fillMaxWidth(),
            painter = painterResource(R.drawable.ic_information_24),
            title = title,
            subtitle = description,
            loading = false,
            onClick = { scope.send(ScreenCommand.Go(sentence)) },
          )
        }
        item { VSpacer(72.dp) }
      }
    }
  }
}
