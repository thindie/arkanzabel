package com.thindie.rknzbl.appfeatures.help.faq

import com.thindie.engine.core.Command

internal sealed interface FaqCommand : Command {
  data object Back : FaqCommand
}
