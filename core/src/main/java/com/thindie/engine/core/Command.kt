package com.thindie.engine.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.thindie.engine.uikit.Action
import java.io.Serializable

@Stable
interface Command

@Stable
interface ViewState : Serializable

sealed interface ServiceCommand : Command {
  data object Dispose : ServiceCommand

  data object DismissError : ServiceCommand

  fun interface Prioritized : ServiceCommand {
    fun execute()
  }

  @Immutable
  sealed interface UiEvent : ServiceCommand {
    @Immutable
    data class Decision(
      val content: @Composable () -> Unit,
      val primaryAction: Action,
      val secondaryAction: Action? = null,
    ) : UiEvent

    @Immutable
    data class Snack(
      val action: Action,
    ) : UiEvent

    @Immutable
    data class SnackText(
      val text: String,
    ) : UiEvent

    @Immutable
    data class Content(val content: @Composable () -> Unit)
  }
}
