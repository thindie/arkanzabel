package com.thindie.rknzbl.feature.managegate.gatelist

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.thindie.rknzbl.R
import com.thindie.rknzbl.engine.Command
import com.thindie.rknzbl.engine.Route
import com.thindie.rknzbl.engine.RouteFactory
import com.thindie.rknzbl.engine.Router
import com.thindie.rknzbl.engine.ScreenFlow
import com.thindie.rknzbl.engine.ScreenScope
import com.thindie.rknzbl.uikit.Action
import com.thindie.rknzbl.uikit.AppScreen
import com.thindie.rknzbl.uikit.AppTheme
import com.thindie.rknzbl.uikit.Button
import com.thindie.rknzbl.uikit.SentenceRow
import com.thindie.rknzbl.uikit.VSpacer

class SelectSourceFlow(
  private val router: Router,
) : ScreenFlow<Route, SelectSourceFlow.Result>(router) {
  override fun start() {
    router.push(main())
  }

  sealed interface Result {
    data object FullBlackShadowSocks : Result
    data object FullBlackVless : Result
    data object MobileBlackVless : Result
    data object WhiteListMobile : Result
    data object WhiteListMobileV2 : Result
    data object WhiteListAll : Result
    data object WhiteListRussian : Result
    data object NotSelected : Result
  }

  fun main() = RouteFactory.create(
    initialState = ScreenState(),
    execute = ::exec,
    routeContent = { HomeScreen() },
  )

  @Immutable
  data class ScreenState(
    val selected: Result = Result.NotSelected,
    val blackSection: List<Result> = listOf(
      Result.FullBlackShadowSocks,
      Result.FullBlackVless,
      Result.MobileBlackVless,
    ),

    val whiteSection: List<Result> = listOf(
      Result.WhiteListMobile,
      Result.WhiteListMobileV2,
      Result.WhiteListAll,
      Result.WhiteListRussian,
    ),
  ) : com.thindie.rknzbl.engine.State

  sealed interface ScreenCommand : Command {
    data object Back : ScreenCommand
    data class Select(val type: Result) : ScreenCommand

  }

  private suspend fun exec(command: ScreenCommand, state: ScreenState): ScreenState {

    return when (command) {
      is ScreenCommand.Back -> {
        finish(state.selected)
        state
      }

      is ScreenCommand.Select -> {
        if (state.selected == command.type) {
          state.copy(selected = Result.NotSelected)
        } else {
          state.copy(selected = command.type)
        }
      }
    }
  }
}

@Composable
fun ScreenScope<SelectSourceFlow.ScreenState, SelectSourceFlow.ScreenCommand>.HomeScreen() {
  val screenState by state.collectAsState()
  AppScreen(
    primary = Action(
      icon = R.drawable.ic_arrow_back_24,
      listener = {
        send(SelectSourceFlow.ScreenCommand.Back)
      }
    )
  ) {
    BackHandler { send(SelectSourceFlow.ScreenCommand.Back) }
    LazyColumn(
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      items(
        state.value.blackSection
      ) { sentence ->
        val (title, description) = sentence.Sentence
        SentenceRow(
          modifier = Modifier
            .border(
              border = BorderStroke(
                width = 1.2.dp,
                color = if (screenState.selected == sentence) {
                  AppTheme.colors.accentPrimary
                } else AppTheme.colors.backgroundSecondary
              ),
              shape = RoundedCornerShape(20.dp)
            )
            .fillMaxWidth(),
          painter = painterResource(R.drawable.ic_information_24),
          title = title,
          subtitle = description,
          loading = false,
          onClick = { send(SelectSourceFlow.ScreenCommand.Select(sentence)) }
        )
      }
      item { VSpacer(56.dp) }
      items(
        state.value.whiteSection
      ) { sentence ->
        val (title, description) = sentence.Sentence
        SentenceRow(
          modifier = Modifier
            .border(
              border = BorderStroke(
                width = 1.2.dp,
                color = if (screenState.selected == sentence) {
                  AppTheme.colors.accentPrimary
                } else AppTheme.colors.backgroundSecondary
              ),
              shape = RoundedCornerShape(20.dp)
            )
            .fillMaxWidth(),
          painter = painterResource(R.drawable.ic_information_24),
          title = title,
          subtitle = description,
          loading = false,
          onClick = { send(SelectSourceFlow.ScreenCommand.Select(sentence)) }
        )
      }
      item {
        val st by state.collectAsState()
        Button(
          modifier = Modifier
            .padding(16.dp),
          enabled = st.selected != SelectSourceFlow.Result.NotSelected,
          text = "готово",
          onClick = { send(SelectSourceFlow.ScreenCommand.Back) }
        )
      }
    }
  }
}

private val SelectSourceFlow.Result.Sentence
  get() = when (this) {
    SelectSourceFlow.Result.FullBlackShadowSocks -> "Черные Списки Shadowsocks+All" to "Все конфиги"
    SelectSourceFlow.Result.FullBlackVless -> "Черные Списки VLESS" to "Все конфиги"
    SelectSourceFlow.Result.MobileBlackVless -> "Черные Списки VLESS" to "150 лучших для телефона"
    SelectSourceFlow.Result.NotSelected -> "Черные Списки VLESS" to "Все конфиги"
    SelectSourceFlow.Result.WhiteListAll -> "Белые Списки / CIDR" to "Все конфиги"
    SelectSourceFlow.Result.WhiteListMobile -> "Белые Списки / CIDR" to "150 лучших для телефона (1)"
    SelectSourceFlow.Result.WhiteListMobileV2 -> "Белые Списки / CIDR" to "150 лучших для телефона (2)"
    SelectSourceFlow.Result.WhiteListRussian -> "Белые Списки / CIDR" to "Конфиги VK, YA, CDN, Beeline"
  }