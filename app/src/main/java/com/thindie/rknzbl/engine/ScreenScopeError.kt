package com.thindie.rknzbl.engine

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.thindie.rknzbl.R

@Immutable
data class ScreenScopeError(
  val message: String,
  val actions: Map<Actions, Command>,
) {
  sealed interface Actions {
    sealed interface Common : Actions {
      @get:StringRes
      val titleRes: Int?

      data object ButtonMain : Common {
        override val titleRes: Int = R.string.error_action_ok
      }

      data object ButtonSecondaryRetry : Common {
        override val titleRes: Int = R.string.error_action_retry
      }

      data object DismissMain : Common {
        override val titleRes: Int? = null
      }
    }
  }
}

val ScreenScopeError.Actions.ref get() =
  when (this) {
    is ScreenScopeError.Actions.Common -> {
      when (this) {
        ScreenScopeError.Actions.Common.ButtonMain -> this.titleRes
        ScreenScopeError.Actions.Common.ButtonSecondaryRetry -> this.titleRes
        ScreenScopeError.Actions.Common.DismissMain -> this.titleRes
      }
    }
  }
