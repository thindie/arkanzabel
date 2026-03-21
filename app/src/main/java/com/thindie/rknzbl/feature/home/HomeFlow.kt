package com.thindie.rknzbl.feature.home

import androidx.activity.compose.BackHandler
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.thindie.rknzbl.engine.Command
import com.thindie.rknzbl.engine.Route
import com.thindie.rknzbl.engine.RouteFactory
import com.thindie.rknzbl.engine.Router
import com.thindie.rknzbl.engine.ScreenFlow
import com.thindie.rknzbl.engine.ScreenScope
import com.thindie.rknzbl.uikit.AppScreen

class HomeFlow(val router: Router): ScreenFlow<Route, Unit>(router) {
  override fun start() {
    router.push(main())
  }

  fun main() = RouteFactory.create(
    initialState = State,
    execute = ::exec,
    routeContent = { HomeScreen() }
  )

  @Immutable
  data object State: com.thindie.rknzbl.engine.State

  sealed interface HomeCommand: Command {
    data object Back: HomeCommand
  }

  fun exec(command: HomeCommand, homeState: State): State {
    when (command) {
      is HomeCommand.Back -> {
        finish(Unit)
      }
      else -> {}
    }
    return homeState
  }
}

@Composable
fun ScreenScope<HomeFlow.State, HomeFlow.HomeCommand>.HomeScreen(
) {
  AppScreen {
    BackHandler { send(HomeFlow.HomeCommand.Back) }
    Text("home", color = Color.White)
  }
}