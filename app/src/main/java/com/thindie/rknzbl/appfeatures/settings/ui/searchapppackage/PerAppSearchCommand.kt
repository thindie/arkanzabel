package com.thindie.rknzbl.appfeatures.settings.ui.searchapppackage

import com.thindie.engine.core.Command

internal sealed interface PerAppSearchCommand : Command {
  data object Back : PerAppSearchCommand

  data object LoadApps : PerAppSearchCommand

  data class SetSearch(val query: String) : PerAppSearchCommand

  data class AddPackage(val packageName: String) : PerAppSearchCommand
}
