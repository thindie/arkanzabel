package com.thindie.engine.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.thindie.engine.core.Command
import com.thindie.engine.core.Route
import com.thindie.engine.core.RouteFactory
import com.thindie.engine.core.ScreenScope
import com.thindie.engine.core.Section
import com.thindie.engine.core.ViewState
import com.thindie.engine.core.stateSink
import com.thindie.engine.core.sub
import com.thindie.engine.core.transition
import com.thindie.engine.uikit.Action
import com.thindie.engine.uikit.AppScreen

/**
 * Example screen demonstrating the Engine pattern.
 * Learn this file and adapt for your own screens:
 *   1. Replace MyState / MyCommand with your types
 *   2. Implement execute() logic
 *   3. Update routeContent composable
 *   4. Always create new files in separate package for:
 *   - Route
 *   - stateSink
 *   - Command
 *   - exec
 *   - ScreenContent
 *
 *   for example, rollout the news feature:
 *   com.package.module.feature.news....list
 *   news:
 *     list:
 *       ListRoute
 *       ListStateSink
 *       ListCommand
 *       ListState
 *       ListExec
 *       ListScreenContent
 */

@Immutable
data class ExampleState(
  val counter: Int = 0,
  val items: List<String> = emptyList(),
) : ViewState

// ── Commands (sealed interface extending Command) ───────────────
sealed interface ExampleCommand : Command {
  data object Increment : ExampleCommand

  data object Decrement : ExampleCommand
}

// ── Subscriptions (external flow → state) ───────────────────────
// Use stateSink + sub().transition() to subscribe to external flows
// and push their emissions into screen state.
internal fun ScreenScope<ExampleState, ExampleCommand>.subscriptions(
  // inject your repository / service here:
  // repository: MyRepository,
) {
  stateSink(this) { scope ->
    // Example: subscribe to a flow of strings (e.g. from a repository)
    // Replace with your actual flow source:
    val sampleFlow = kotlinx.coroutines.flow.flowOf("item1", "item2", "item3")

    scope.sub(sampleFlow).transition(
      block = { prevState, newItem ->
        prevState.copy(
          items = prevState.items + newItem,
        )
      },
    )
  }
}

// ── Example with repository subscription ────────────────────────
// This shows how to subscribe to a repository flow and update state
internal fun ScreenScope<ExampleState, ExampleCommand>.subscribeToRepository(
  // repository: MyRepository,
) {
  stateSink(this) { scope ->
    // Replace with your actual repository call:
    // val forecastFlow = repository.getForecast()
    val sampleFlow = kotlinx.coroutines.flow.flowOf("data1", "data2")

    scope.sub(sampleFlow).transition(
      block = { prevState, newData ->
        // Transform the incoming data into state updates
        prevState.copy(
          // or however you want to update
          items = listOf(newData),
        )
      },
    )
  }
}

// ── Route factory call ──────────────────────────────────────────
fun createExampleRoute(): Route {
  return RouteFactory.create<ExampleCommand, ExampleState>(
    id = "example_screen",
    initialState = ExampleState(),
    // ── Commands vs Subscriptions — pick ONE per piece of state ───
    //
    // 1. COMMAND-DRIVEN (execute block):
    //    State updates are triggered explicitly by sending commands:
    //      scope.send(MyCommand.LoadData)
    //    The execute() block processes the command and returns new state.
    //    Good for: user actions, explicit data fetches, form submissions.
    //
    // 2. REACTIVE (subscriptions via stateSink):
    //    State updates are driven by external flows automatically:
    //      scope.sub(repository.getForecast()).transition { ... }
    //    No command needed — the flow emission triggers the update.
    //    Good for: live data streams, repository observables, real-time updates.
    //
    // ⚠️ DO NOT mix both patterns for the same state field!
    // If you use subscriptions to update `items`, don't also try to
    // update it via execute(). The two paths race each other:
    //   - command path: mutex-locked, sequential
    //   - subscription path: async flow emissions
    // Result: lost updates, inconsistent state.
    //
    // Rule of thumb:
    //   - User-triggered actions → commands (execute)
    //   - External data streams → subscriptions (stateSink + sub().transition())
    execute = { command, state ->
      when (command) {
        ExampleCommand.Increment -> state.copy(counter = state.counter + 1)
        ExampleCommand.Decrement -> state.copy(counter = state.counter - 1)
      }
    },
    // will be without bottom bar
    section = Section.Leaf,
    routeContent = { scope: ScreenScope<ExampleState, ExampleCommand> ->
      AppScreen(
        screenScope = scope,
        title = "Example Screen",
        subtitle = "Engine Pattern Demo",
        primary =
          Action(
            listener = { scope.send(ExampleCommand.Decrement) },
            // R.drawable.my_ref
            resRef = 1,
          ),
      ) {
        // the loading / errors will be processed automatically inside AppScreen
        val state by scope.state.collectAsState()
        Column(modifier = Modifier.fillMaxSize()) {
          Text("Counter: ${state.counter}")
        }
      }
    },
  )
}
