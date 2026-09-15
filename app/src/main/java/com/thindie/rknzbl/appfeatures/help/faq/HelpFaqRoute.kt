package com.thindie.rknzbl.appfeatures.help.faq

import com.thindie.engine.core.RouteFactory
import com.thindie.rknzbl.appfeatures.help.HelpFlow

fun HelpFlow.helpFaq() =
  RouteFactory.create(
    initialState = FaqState(),
    execute = { c: FaqCommand, s: FaqState ->
      when (c) {
        is FaqCommand.Back -> {
          back()
          null
        }
      }
    },
    id = "HelpFlow-helpFaq",
    routeContent = ::FaqScreenContent,
  )
