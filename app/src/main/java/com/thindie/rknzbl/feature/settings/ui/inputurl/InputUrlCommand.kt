package com.thindie.rknzbl.feature.settings.ui.inputurl

import com.thindie.engine.core.Command

internal sealed interface InputUrlCommand : Command {
  data object Back : InputUrlCommand

  data class SetUrl(val url: String) : InputUrlCommand

  data object Done : InputUrlCommand
}
