package com.thindie.rknzbl.appfeatures.settings.ui.licenses

import com.thindie.engine.core.Command

internal sealed interface LicensesCommand : Command {
  data object Back : LicensesCommand
}
