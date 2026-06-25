package com.thindie.rknzbl.feature.intro

import com.thindie.rknzbl.R
import com.thindie.rknzbl.engine.RouteFactory
import com.thindie.rknzbl.engine.ScreenScopeError

fun IntroFlow.main() =
  RouteFactory.create(
    initialState = State(),
    execute = ::exec,
    routeContent = ::IntroScreenContent,
    errorMapper = {
      ScreenScopeError(
        message = appContext.getString(R.string.error_unexpected),
        actions = emptyMap(),
      )
    },
    id = "IntroFlow-vpn",
    initialCommand =
      RouteFactory.InitialCommand {
        CommandIntro.Start as CommandIntro
      },
  )

private suspend fun IntroFlow.exec(
  command: CommandIntro,
  state: State,
): State {
  return when (command) {
    CommandIntro.Start -> {
      when {
        !hasVpnPermission() ->
          state.copy(stage = Stage.SoftRequest, current = Permission.Vpn, hint = null)

        !hasPushPermission ->
          state.copy(stage = Stage.SoftRequest, current = Permission.Push, hint = null)

        else -> {
          finish(Result.Success)
          state
        }
      }
    }

    CommandIntro.AcceptSoftRequest ->
      state.copy(stage = Stage.Rationale, hint = null)

    CommandIntro.DeclineSoftRequest ->
      state.copy(
        stage = Stage.SoftRequest,
        hint =
          when (state.current) {
            Permission.Vpn -> appContext.getString(R.string.intro_hint_vpn_decline)
            Permission.Push -> appContext.getString(R.string.intro_hint_push_decline)
          },
      )

    CommandIntro.ConfirmRationale -> {
      when (state.current) {
        Permission.Vpn -> {
          if (hasVpnPermission()) {
            state.copy(current = Permission.Push, stage = Stage.SoftRequest)
          } else {
            state.copy(
              stage = Stage.SoftRequest,
              hint = appContext.getString(R.string.intro_hint_vpn_not_granted),
            )
          }
        }

        Permission.Push -> {
          finish(Result.Success)
          state
        }
      }
    }

    CommandIntro.PermissionDenied -> {
      when (state.current) {
        Permission.Vpn ->
          state.copy(
            stage = Stage.SoftRequest,
            hint = appContext.getString(R.string.intro_hint_vpn_not_granted),
          )

        Permission.Push ->
          state.copy(stage = Stage.RationaleDismissedOnce)
      }
    }

    CommandIntro.Dismiss -> {
      when (state.current) {
        Permission.Vpn -> Unit
        Permission.Push ->
          when (state.stage) {
            Stage.Loading -> Unit
            Stage.Rationale -> finish(Result.Success)
            Stage.SoftRequest -> finish(Result.Success)
            Stage.RationaleDismissedOnce -> finish(Result.Success)
          }
      }
      state
    }
  }
}
