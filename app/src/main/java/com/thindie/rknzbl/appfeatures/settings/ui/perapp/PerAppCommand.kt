package com.thindie.rknzbl.appfeatures.settings.ui.perapp

import com.thindie.engine.core.Command

internal sealed interface PerAppProxyCommand : Command {
  data object Back : PerAppProxyCommand

  data object LoadApps : PerAppProxyCommand

  data object SetModeAll : PerAppProxyCommand

  data object SetModeSelected : PerAppProxyCommand

  data object OpenSearch : PerAppProxyCommand

  data class RemovePackage(val packageName: String) : PerAppProxyCommand
}
