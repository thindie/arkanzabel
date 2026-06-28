package com.thindie.rknzbl.feature.perapp

import com.thindie.rknzbl.engine.Command

sealed interface PerAppProxyCommand : Command {
  data object Back : PerAppProxyCommand

  data object LoadApps : PerAppProxyCommand

  data object RefreshFromStorage : PerAppProxyCommand

  data object SetModeAll : PerAppProxyCommand

  data object SetModeSelected : PerAppProxyCommand

  data object OpenSearch : PerAppProxyCommand

  data class RemovePackage(val packageName: String) : PerAppProxyCommand
}

sealed interface PerAppSearchCommand : Command {
  data object Back : PerAppSearchCommand

  data object LoadApps : PerAppSearchCommand

  data class SetSearch(val query: String) : PerAppSearchCommand

  data class AddPackage(val packageName: String) : PerAppSearchCommand
}
