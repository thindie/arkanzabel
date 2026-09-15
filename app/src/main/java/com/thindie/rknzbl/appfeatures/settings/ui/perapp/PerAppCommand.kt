package com.thindie.rknzbl.appfeatures.settings.ui.perapp

import com.thindie.engine.core.Command

internal sealed interface PerAppCommand : Command {
  data object Back : PerAppCommand

  data object SetModeAll : PerAppCommand

  data object SetModeSelected : PerAppCommand

  data object OpenSearch : PerAppCommand

  data class RemovePackage(val packageName: String) : PerAppCommand
}
