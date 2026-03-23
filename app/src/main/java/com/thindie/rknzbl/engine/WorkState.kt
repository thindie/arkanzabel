package com.thindie.rknzbl.engine

import androidx.compose.runtime.Immutable

@Immutable
interface WorkState {
  @Immutable
  data object Idle : WorkState

  @Immutable
  data object Running : WorkState

  @Immutable
  data class Error(val message: String, val cause: Throwable? = null) : WorkState
}
