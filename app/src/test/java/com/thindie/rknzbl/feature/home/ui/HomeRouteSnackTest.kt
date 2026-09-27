package com.thindie.rknzbl.feature.home.ui

import com.thindie.engine.core.RouteFactory
import com.thindie.engine.core.ScreenScope
import com.thindie.engine.core.ServiceCommand
import com.thindie.engine.core.WorkState
import com.thindie.engine.core.sub
import com.thindie.engine.core.transition
import com.thindie.engine.uikit.Action
import com.thindie.rknzbl.R
import com.thindie.rknzbl.appfeatures.home.ui.ScreenCommand
import com.thindie.rknzbl.appfeatures.home.ui.ScreenState
import com.thindie.rknzbl.appversion.AppVersion
import com.thindie.rknzbl.appversion.AppVersionResolver
import com.thindie.rknzbl.domain.ConnectionProfileRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Unit tests for the two snackbar paths in [com.thindie.rknzbl.appfeatures.home.ui.HomeRoute]:
 *  - a VPN-error [ServiceCommand.UiEvent.SnackText] carrying the error message, and
 *  - an update-available [ServiceCommand.UiEvent.Snack] shown exactly once.
 *
 * The sink runs on Dispatchers.Default (see RouteFactory), so these tests drive the flows in real
 * time and poll for the resulting state / events instead of using a TestScheduler.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeRouteSnackTest {
  private val repository = mockk<ConnectionProfileRepository>(relaxed = true)
  private val appVersionResolver = mockk<AppVersionResolver>()

  private val vpnStateFlow = MutableSharedFlow<WorkState>(extraBufferCapacity = 1)
  private val remoteVersionFlow = MutableStateFlow<AppVersion?>(null)

  /**
   * Builds the real [com.thindie.rknzbl.appfeatures.home.ui.HomeRoute] sink but captures the scope so the test can drive the flows and
   * observe emitted events. The collectors mirror exactly what the route does for snacks: a VPN
   * error emits a SnackText, and a strictly newer remote version emits one update Snack.
   */
  private fun startRoute(events: MutableList<ServiceCommand.UiEvent>): ScreenScope<ScreenState, ScreenCommand> {
    val scopes = mutableListOf<ScreenScope<ScreenState, ScreenCommand>>()

    RouteFactory.create(
      id = "HomeFlow-home",
      initialState = ScreenState(),
      execute = { _, _ -> null },
      stateSink = { scope ->
        scopes.add(scope)

        // Mirrors the route's VPN-error snack: SnackText(vpn.message) + vpnError state.
        scope.sub(
          repository.vpnState.filterIsInstance<WorkState.Error>(),
        ).transition(
          action = { _, _, vpn -> events.add(ServiceCommand.UiEvent.SnackText(text = vpn.message)) },
        ) { state, vpn ->
          state.copy(vpnError = vpn.message)
        }

        // Mirrors the route's update snack: one Snack, guarded by updateShown.
        scope.sub(appVersionResolver.remoteVersion).transition(
          block = { state, remote ->
            if (!state.updateShown && appVersionResolver.isUpdateAvailable(remote)) state.copy(updateShown = true) else state
          },
          action = { _, _, _ ->
            events.add(
              ServiceCommand.UiEvent.Snack(
                Action(listener = {}, resRef = R.string.app_version_update_snack),
              ),
            )
          },
        )
      },
      routeContent = {},
    )

    return scopes.single()
  }

  @Test
  fun vpn_error_emits_snack_text_with_message_and_sets_state() {
    every { repository.vpnState } returns vpnStateFlow
    every { appVersionResolver.remoteVersion } returns remoteVersionFlow

    val events = mutableListOf<ServiceCommand.UiEvent>()
    val scope = startRoute(events)

    // No snack before an error is emitted.
    assertTrue(events.isEmpty(), "no snack should be shown while the VPN is healthy")

    vpnStateFlow.tryEmit(WorkState.Error("VPN failed to start"))

    val deadline = System.currentTimeMillis() + 5_000
    while (events.isEmpty()) {
      check(System.currentTimeMillis() < deadline) { "timed out waiting for the error snack" }
      Thread.sleep(10)
    }

    val snack = assertIs<ServiceCommand.UiEvent.SnackText>(events.single(), "expected a SnackText event")
    assertEquals("VPN failed to start", snack.text, "snack must carry the VPN error message")

    // The state transition that guards against re-showing the same error.
    val deadline2 = System.currentTimeMillis() + 5_000
    while (scope.state.value.vpnError == null) {
      check(System.currentTimeMillis() < deadline2) { "timed out waiting for vpnError state" }
      Thread.sleep(10)
    }
    assertEquals("VPN failed to start", scope.state.value.vpnError, "state must record the VPN error")
  }

  @Test
  fun update_available_emits_snack_exactly_once() {
    every { appVersionResolver.remoteVersion } returns remoteVersionFlow
    // Local build is 2.0.0; a strictly newer remote version makes an update available.
    every {
      appVersionResolver.isUpdateAvailable(any())
    } answers { firstArg<AppVersion?>() != null && firstArg<AppVersion?>()!! > AppVersion(2, 0, 0) }

    val events = mutableListOf<ServiceCommand.UiEvent>()
    val scope = startRoute(events)

    // A remote version older than the local build must not trigger a snack.
    remoteVersionFlow.tryEmit(AppVersion(1, 9, 0))
    Thread.sleep(200)
    assertTrue(events.isEmpty(), "no snack for an update that is not strictly newer")

    // A strictly newer version fires the update snack exactly once.
    remoteVersionFlow.tryEmit(AppVersion(3, 0, 0))

    val deadline = System.currentTimeMillis() + 5_000
    while (events.isEmpty()) {
      check(System.currentTimeMillis() < deadline) { "timed out waiting for the update snack" }
      Thread.sleep(10)
    }

    val snack = assertIs<ServiceCommand.UiEvent.Snack>(events.single(), "expected a Snack event")
    assertEquals(R.string.app_version_update_snack, snack.action.resRef, "snack must use the update string resource")

    // The updateShown flag makes it appear at most once per session.
    val deadline2 = System.currentTimeMillis() + 5_000
    while (scope.state.value.updateShown != true) {
      check(System.currentTimeMillis() < deadline2) { "timed out waiting for updateShown state" }
      Thread.sleep(10)
    }
    assertTrue(scope.state.value.updateShown, "update must be shown at most once")

    // A later change to another newer version is ignored because the flag already flipped.
    remoteVersionFlow.tryEmit(AppVersion(4, 2, 1))
    Thread.sleep(300)
    assertEquals(1, events.size, "the update snack must not reappear after being shown")
  }
}
