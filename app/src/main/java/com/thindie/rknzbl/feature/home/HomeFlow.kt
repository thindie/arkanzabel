package com.thindie.rknzbl.feature.home

import android.content.Context
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.thindie.rknzbl.BuildConfig
import com.thindie.rknzbl.R
import com.thindie.rknzbl.engine.Command
import com.thindie.rknzbl.engine.Route
import com.thindie.rknzbl.engine.RouteFactory
import com.thindie.rknzbl.engine.Router
import com.thindie.rknzbl.engine.ScreenFlow
import com.thindie.rknzbl.engine.ScreenScope
import com.thindie.rknzbl.engine.ScreenScopeError
import com.thindie.rknzbl.uikit.AppScreen
import com.v2ray.ang.runtime.KeyValueStorage
import com.v2ray.ang.runtime.ProfileUriParser
import com.v2ray.ang.runtime.V2RayServiceManager

class HomeFlow(
  private val router: Router,
  private val appContext: Context,
) : ScreenFlow<Route, Unit>(router) {

  private companion object {
    private const val COMMAND_STATE_LOG_TAG = "Arknzbl.HomeCommandState"
    private const val VPN_START_DIAG_TAG = "Arknzbl.VpnStartDiag"
  }
  override fun start() {
    router.push(main())
  }

  fun main() = RouteFactory.create(
    initialState = State(),
    execute = { cmd, st -> exec(cmd, st) },
    errorMapper = { e ->
      if (BuildConfig.DEBUG) {
        Log.e(COMMAND_STATE_LOG_TAG, "Home screen command failed", e)
      }
      val detail = e.message?.trim()
      val text =
        if (!detail.isNullOrEmpty()) {
          appContext.getString(
            R.string.vpn_screen_error_detail,
            e.javaClass.simpleName,
            detail,
          )
        } else {
          appContext.getString(R.string.vpn_screen_error_generic, e.javaClass.simpleName)
        }
      ScreenScopeError(
        message = text,
        actions = mapOf(
          ScreenScopeError.Actions.Common.ButtonMain to HomeCommand.DismissScreenError,
        ),
      )
    },
    routeContent = { HomeScreen() },
  )

  @Immutable
  data class State(
    val hint: String? = null,
  ) : com.thindie.rknzbl.engine.State

  sealed interface HomeCommand : Command {
    data object Back : HomeCommand

    data class ImportProfile(val uri: String) : HomeCommand

    data object VpnStartAfterPrepare : HomeCommand

    data object StopVpn : HomeCommand

    data class SetMessage(val text: String) : HomeCommand

    /** Clears [RouteFactory] error overlay after an unexpected failure. */
    data object DismissScreenError : HomeCommand
  }

  private suspend fun exec(command: HomeCommand, homeState: State): State {
    val newState = when (command) {
      is HomeCommand.Back -> {
        finish(Unit)
        homeState
      }

      is HomeCommand.ImportProfile -> {
        val profile = ProfileUriParser.parse(command.uri)
        if (profile == null) {
          homeState.copy(hint = appContext.getString(R.string.vpn_import_fail))
        } else {
          try {
            KeyValueStorage.encodeServerConfig("", profile)
            homeState.copy(hint = appContext.getString(R.string.vpn_import_ok))
          } catch (e: IllegalStateException) {
            if (BuildConfig.DEBUG) {
              Log.e(COMMAND_STATE_LOG_TAG, "ImportProfile storage/serialize failed", e)
            }
            val hint =
              when {
                e.message == KeyValueStorage.ERROR_MESSAGE_PROFILE_JSON_FAILED ->
                  appContext.getString(R.string.vpn_import_save_failed_json)
                else -> {
                  val m = e.message?.trim()
                  if (!m.isNullOrEmpty()) {
                    appContext.getString(R.string.vpn_import_save_failed_storage_detail, m)
                  } else {
                    appContext.getString(R.string.vpn_import_save_failed_storage)
                  }
                }
              }
            homeState.copy(hint = hint)
          } catch (e: UnsatisfiedLinkError) {
            if (BuildConfig.DEBUG) {
              Log.e(COMMAND_STATE_LOG_TAG, "ImportProfile MMKV native", e)
            }
            homeState.copy(hint = appContext.getString(R.string.vpn_import_save_failed_native))
          } catch (e: OutOfMemoryError) {
            throw e
          } catch (e: RuntimeException) {
            if (BuildConfig.DEBUG) {
              Log.e(COMMAND_STATE_LOG_TAG, "ImportProfile save failed", e)
            }
            val m = e.message?.trim()
            val hint =
              if (!m.isNullOrEmpty()) {
                appContext.getString(
                  R.string.vpn_import_save_failed_detail,
                  e.javaClass.simpleName,
                  m,
                )
              } else {
                appContext.getString(
                  R.string.vpn_import_save_failed_other,
                  e.javaClass.simpleName,
                )
              }
            homeState.copy(hint = hint)
          }
        }
      }

      is HomeCommand.VpnStartAfterPrepare -> {
        val selected = KeyValueStorage.getSelectServer()
        val hasProfile =
          !selected.isNullOrBlank() && KeyValueStorage.decodeServerConfig(selected) != null
        if (!hasProfile) {
          homeState.copy(hint = appContext.getString(R.string.vpn_start_need_profile))
        } else {
          V2RayServiceManager.startVService(appContext)
          if (BuildConfig.DEBUG) {
            Log.d(
              VPN_START_DIAG_TAG,
              "FGS requested; core isRunning=${V2RayServiceManager.isRunning()} (early snapshot). " +
                "If no VPN key: logcat process :RunSoLibV2RayDaemon",
            )
          }
          homeState
        }
      }

      is HomeCommand.StopVpn -> {
        V2RayServiceManager.stopVService(appContext)
        homeState
      }

      is HomeCommand.SetMessage ->
        homeState.copy(hint = command.text)

      is HomeCommand.DismissScreenError ->
        homeState
    }
    Log.d(COMMAND_STATE_LOG_TAG, "${command.forLog()}: $homeState -> $newState")
    return newState
  }

  private fun HomeCommand.forLog(): String =
    when (this) {
      is HomeCommand.ImportProfile ->
        "ImportProfile(uriLength=${uri.length})"
      else ->
        toString()
    }
}

@Composable
fun ScreenScope<HomeFlow.State, HomeFlow.HomeCommand>.HomeScreen() {
  AppScreen {
    BackHandler { send(HomeFlow.HomeCommand.Back) }
    HomeVpnMvpPanel()
  }
}
