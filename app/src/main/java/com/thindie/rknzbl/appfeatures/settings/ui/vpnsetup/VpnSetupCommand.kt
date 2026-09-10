package com.thindie.rknzbl.appfeatures.settings.ui.vpnsetup

import com.thindie.engine.core.Command

internal sealed interface VpnSetupCommand : Command {
  data object Back : VpnSetupCommand
}
