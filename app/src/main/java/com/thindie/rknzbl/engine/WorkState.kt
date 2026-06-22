package com.thindie.rknzbl.engine

import androidx.compose.runtime.Immutable

@Immutable
sealed interface WorkState {
  @Immutable
  data object NotRunning : WorkState

  @Immutable
  data object Running : WorkState

  @Immutable
  data class Error(val message: String, val cause: Throwable? = null) : WorkState
}
