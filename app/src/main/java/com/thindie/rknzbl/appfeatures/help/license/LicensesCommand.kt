package com.thindie.rknzbl.appfeatures.help.license

import com.thindie.engine.core.Command

internal sealed interface LicensesCommand : Command {
  data object Back : LicensesCommand
}
