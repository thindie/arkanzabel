package com.thindie.rknzbl.error

sealed class AppError : Exception() {
  data class UnexpectedError(
    override val cause: Throwable?,
    override val message: String?,
  ) : AppError()

  sealed class ServerError : AppError() {
    data object TimeOut : ServerError()
  }
}
