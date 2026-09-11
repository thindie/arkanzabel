package com.thindie.rknzbl.appfeatures.help.vpnsetup

import com.thindie.engine.core.Command

internal sealed interface VpnSetupCommand : Command {
  data object Back : VpnSetupCommand
}
