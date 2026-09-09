package com.thindie.rknzbl.appfeatures.settings.ui.faq

import com.thindie.engine.core.Command

internal sealed interface FaqCommand : Command {
  data object Back : FaqCommand
}
