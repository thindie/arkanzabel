package com.thindie.rknzbl.appfeatures.profiles.deletesaved

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
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
import com.thindie.engine.uikit.Button
import com.thindie.engine.uikit.SentenceRow
import com.thindie.engine.uikit.VSpacer
import com.thindie.rknzbl.R

@Composable
internal fun DeleteProfilesScreen(scope: ScreenScope<DeleteSavedProfilesState, DeleteSavedProfilesCommand>) {
  val st by scope.state.collectAsState()
  BackHandler { scope.send(DeleteSavedProfilesCommand.Exit) }
  AppScreen(
    screenScope = scope,
    title = null,
    primary =
      Action(
        resRef = R.drawable.ic_arrow_back_24,
        listener = { scope.send(DeleteSavedProfilesCommand.Exit) },
      ),
  ) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
      Text(
        text = stringResource(R.string.profiles_delete_title),
        style = AppTheme.typography.headlineLarge,
        color = AppTheme.colors.contentPrimary,
      )
      VSpacer(24.dp)
      if (st.savedProfiles.isEmpty()) {
        Text(
          text = stringResource(R.string.profiles_saved_empty),
          style = AppTheme.typography.bodyMedium,
          color = AppTheme.colors.contentSecondary,
          modifier = Modifier.fillMaxWidth(),
        )
      } else {
        LazyColumn(modifier = Modifier.weight(1f)) {
          items(items = st.savedProfiles) { profile ->
            val selected = profile in st.selectedProfiles
            SentenceRow(
              modifier = Modifier.fillMaxWidth(),
              painter =
                if (selected) {
                  painterResource(R.drawable.ic_done_square_24)
                } else {
                  painterResource(R.drawable.ic_internet_24)
                },
              title = profile.remarks,
              subtitle = profile.flow ?: profile.server.orEmpty(),
              loading = false,
              onClick = { scope.send(DeleteSavedProfilesCommand.ToggleSelect(profile)) },
            )
            VSpacer(8.dp)
          }
        }
        VSpacer(24.dp)
        Button(
          enabled = st.selectedProfiles.isNotEmpty(),
          text =
            if (st.selectedProfiles.isEmpty()) {
              stringResource(R.string.source_stored_delete_hint)
            } else {
              stringResource(R.string.source_stored_selected_count, st.selectedProfiles.size)
            },
          onClick = { scope.send(DeleteSavedProfilesCommand.ConfirmDelete) },
        )
      }
    }
  }
}
