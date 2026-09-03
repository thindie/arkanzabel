package com.thindie.rknzbl.appfeatures.settings.ui.source

import com.thindie.engine.core.Command

internal sealed interface SourceCommand : Command {
  data object Back : SourceCommand

  data class SelectPreset(val url: String, val name: String) : SourceCommand

  data class SetCustomUrl(val url: String) : SourceCommand

  data object ClearSource : SourceCommand
}
