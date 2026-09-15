package com.thindie.rknzbl.appfeatures.help

import com.thindie.engine.core.Command

internal sealed interface HelpCommand : Command {
  data object Back : HelpCommand

  data object OpenFaq : HelpCommand

  data object OpenLicenses : HelpCommand

  data object OpenVpnSetup : HelpCommand
}
