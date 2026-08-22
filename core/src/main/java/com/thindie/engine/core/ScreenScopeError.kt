package com.thindie.engine.core

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable

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
        override val titleRes: Int? = null
      }

      data object ButtonSecondaryRetry : Common {
        override val titleRes: Int? = null
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
