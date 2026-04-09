package com.thindie.rknzbl.feature.managegate.storedgates

import android.content.Context
import com.thindie.rknzbl.R
import com.thindie.rknzbl.engine.Route
import com.thindie.rknzbl.engine.Router
import com.thindie.rknzbl.engine.ScreenFlow
import com.thindie.rknzbl.engine.ScreenScopeError
import com.thindie.rknzbl.engine.ServiceCommand
import com.thindie.rknzbl.error.AppError
import com.thindie.rknzbl.feature.home.domain.ConnectionProfileRepository
import com.thindie.rknzbl.feature.managegate.storedgates.profiles.profiles

class FavoriteProfilesFlow(
  private val router: Router,
  val repository: ConnectionProfileRepository,
  val appContext: Context,
) : ScreenFlow<Route, Unit>(router) {

  override fun start() {
    go(profiles())
  }


  private fun webDavErrorMessage(t: Throwable): String =
    when (t) {
      is AppError.WebDav.Unauthorized -> appContext.getString(R.string.webdav_error_unauthorized)
      is AppError.WebDav.Forbidden -> appContext.getString(R.string.webdav_error_forbidden)
      is AppError.WebDav.NotFound ->
        t.requestedUrl?.let { url ->
          appContext.getString(R.string.webdav_error_not_found_url, url)
        } ?: appContext.getString(R.string.webdav_error_not_found)

      is AppError.WebDav.Conflict -> appContext.getString(R.string.webdav_error_conflict)
      is AppError.WebDav.InvalidPropfindResponse -> appContext.getString(R.string.webdav_error_invalid_response)
      is AppError.WebDav.UploadOpenFailed -> appContext.getString(R.string.webdav_error_upload_open)
      else -> appContext.getString(R.string.error_unexpected)
    }

  fun errorMapper(e: Throwable): ScreenScopeError {
    return when (e) {
      is AppError -> {
        when (e) {
          is AppError.ServerError.HttpRequestFailed -> {
            ScreenScopeError(
              message = webDavErrorMessage(e),
              actions = mapOf(
                ScreenScopeError.Actions.Common.ButtonMain to ServiceCommand.DismissError
              )
            )
          }

          AppError.ServerError.TimeOut -> {
            ScreenScopeError(
              message = webDavErrorMessage(e),
              actions = mapOf(
                ScreenScopeError.Actions.Common.ButtonMain to ServiceCommand.DismissError
              )
            )
          }

          AppError.ServerError.ConnectionFailed -> {
            ScreenScopeError(
              message = webDavErrorMessage(e),
              actions = mapOf(
                ScreenScopeError.Actions.Common.ButtonMain to ServiceCommand.DismissError
              )
            )
          }

          is AppError.UnexpectedError -> {
            ScreenScopeError(
              message = webDavErrorMessage(e),
              actions = mapOf(
                ScreenScopeError.Actions.Common.ButtonMain to ServiceCommand.DismissError
              )
            )
          }

          AppError.WebDav.Conflict,
          AppError.WebDav.Forbidden,
          AppError.WebDav.InvalidPropfindResponse,
          is AppError.WebDav.NotFound,
          AppError.WebDav.Unauthorized,
          AppError.WebDav.UploadOpenFailed,
            -> {
            ScreenScopeError(
              message = webDavErrorMessage(e),
              actions = mapOf(
                ScreenScopeError.Actions.Common.ButtonMain to ServiceCommand.DismissError
              )
            )
          }
        }
      }

      else -> ScreenScopeError(
        message = webDavErrorMessage(e),
        actions = mapOf(
          ScreenScopeError.Actions.Common.ButtonMain to ServiceCommand.DismissError
        )
      )
    }
  }
}
