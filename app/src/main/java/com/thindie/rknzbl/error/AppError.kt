package com.thindie.rknzbl.error

sealed class AppError : Exception() {
  data class UnexpectedError(
    override val cause: Throwable?,
    override val message: String?,
  ) : AppError()

  sealed class ServerError : AppError() {
    data object TimeOut : ServerError()
    data object ConnectionFailed : ServerError()
    data class HttpRequestFailed(val statusCode: Int) : ServerError()
  }

  sealed class WebDav : AppError() {
    data object Unauthorized : WebDav()
    data object Forbidden : WebDav()
    data class NotFound(val requestedUrl: String? = null) : WebDav()
    data object Conflict : WebDav()
    data object InvalidPropfindResponse : WebDav()
    data object UploadOpenFailed : WebDav()
  }
}
