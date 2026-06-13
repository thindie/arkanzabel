package com.thindie.rknzbl.feature.managegate.gatelist

import android.content.Context
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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.thindie.rknzbl.R
import com.thindie.rknzbl.engine.Command
import com.thindie.rknzbl.engine.Route
import com.thindie.rknzbl.engine.RouteFactory
import com.thindie.rknzbl.engine.Router
import com.thindie.rknzbl.engine.ScreenFlow
import com.thindie.rknzbl.engine.ScreenScope
import com.thindie.rknzbl.engine.ScreenScopeError
import com.thindie.rknzbl.uikit.Action
import com.thindie.rknzbl.uikit.AppScreen
import com.thindie.rknzbl.uikit.AppTheme
import com.thindie.rknzbl.uikit.Button
import com.thindie.rknzbl.uikit.SentenceRow
import com.thindie.rknzbl.uikit.VSpacer

class SelectSourceFlow(
  private val router: Router,
  private val appContext: Context,
) : ScreenFlow<Route, SelectSourceFlow.Result>(router) {
  override fun start() {
    go(main())
  }

  sealed interface Result {
    val sourceUrl: String?

    data object FullBlackShadowSocks : Result {
      override val sourceUrl: String =
        "https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/refs/heads/main/BLACK_SS%2BAll_RUS.txt"
    }

    data object FullBlackVless : Result {
      override val sourceUrl: String =
        "https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/refs/heads/main/BLACK_VLESS_RUS.txt"
    }

    data object MobileBlackVless : Result {
      override val sourceUrl: String =
        "https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/refs/heads/main/BLACK_VLESS_RUS_mobile.txt"
    }

    data object WhiteListMobile : Result {
      override val sourceUrl: String =
        "https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/refs/heads/main/Vless-Reality-White-Lists-Rus-Mobile.txt"
    }

    data object WhiteListMobileV2 : Result {
      override val sourceUrl: String =
        "https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/refs/heads/main/Vless-Reality-White-Lists-Rus-Mobile-2.txt"
    }

    data object WhiteListAll : Result {
      override val sourceUrl: String =
        "https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/refs/heads/main/WHITE-CIDR-RU-all.txt"
    }

    data object WhiteListRussian : Result {
      override val sourceUrl: String =
        "https://github.com/igareck/vpn-configs-for-russia/blob/main/WHITE-CIDR-RU-checked.txt"
    }

    data object NotSelected : Result {
      override val sourceUrl: String? = null
    }

    data object StoredProfiles: Result {
      override val sourceUrl: String?
        get() = null
    }
  }

  fun main() = RouteFactory.create(
    initialState = ScreenState(),
    execute = ::exec,
    routeContent = { HomeScreen() },
    id = "Select-main",
    errorMapper = {
      ScreenScopeError(
        message = appContext.getString(R.string.error_unexpected),
        actions = emptyMap(),
      )
    },
  )

  @Immutable
  data class ScreenState(
    val selected: Result = Result.NotSelected,
    val blackSection: List<Result> = listOf(
      Result.FullBlackShadowSocks,
      Result.FullBlackVless,
      Result.MobileBlackVless,
      Result.StoredProfiles
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
  val context = LocalContext.current
  AppScreen(
    primary = Action(
      resRef = R.drawable.ic_arrow_back_24,
      listener = {
        send(SelectSourceFlow.ScreenCommand.Back)
      }
    )
  ) {
    Box {
      BackHandler { send(SelectSourceFlow.ScreenCommand.Back) }
      LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        items(
          state.value.blackSection
        ) { sentence ->
          val (title, description) = sentence.resolveLabels(context)
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
          val (title, description) = sentence.resolveLabels(context)
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
          VSpacer(72.dp)
        }
      }
      val st by state.collectAsState()
      Button(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(16.dp),
        enabled = st.selected != SelectSourceFlow.Result.NotSelected,
        text = stringResource(R.string.source_select_done),
        onClick = { send(SelectSourceFlow.ScreenCommand.Back) }
      )
    }
  }
}

internal fun SelectSourceFlow.Result.resolveLabels(context: Context): Pair<String, String> =
  when (this) {
    SelectSourceFlow.Result.FullBlackShadowSocks ->
      context.getString(R.string.source_black_ss_title) to
        context.getString(R.string.source_subtitle_all_configs)

    SelectSourceFlow.Result.FullBlackVless,
    SelectSourceFlow.Result.NotSelected,
      ->
      context.getString(R.string.source_black_vless_title) to
        context.getString(R.string.source_subtitle_all_configs)

    SelectSourceFlow.Result.MobileBlackVless ->
      context.getString(R.string.source_black_vless_title) to
        context.getString(R.string.source_subtitle_top150_phone)

    SelectSourceFlow.Result.WhiteListAll ->
      context.getString(R.string.source_white_cidr_title) to
        context.getString(R.string.source_subtitle_all_configs)

    SelectSourceFlow.Result.WhiteListMobile ->
      context.getString(R.string.source_white_cidr_title) to
        context.getString(R.string.source_subtitle_top150_phone_1)

    SelectSourceFlow.Result.WhiteListMobileV2 ->
      context.getString(R.string.source_white_cidr_title) to
        context.getString(R.string.source_subtitle_top150_phone_2)

    SelectSourceFlow.Result.WhiteListRussian ->
      context.getString(R.string.source_white_cidr_title) to
        context.getString(R.string.source_subtitle_ru_services)

    SelectSourceFlow.Result.StoredProfiles -> context.getString(
      R.string.source_stored
    ) to context.getString(R.string.source_subtitle_stored)
  }