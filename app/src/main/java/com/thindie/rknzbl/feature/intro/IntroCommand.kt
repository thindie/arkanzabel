package com.thindie.rknzbl.feature.intro

import com.thindie.rknzbl.engine.Command

sealed interface CommandIntro : Command {
  data object Start : CommandIntro

  data object Dismiss : CommandIntro

  data object AcceptSoftRequest : CommandIntro

  data object DeclineSoftRequest : CommandIntro

  data object ConfirmRationale : CommandIntro

  data object PermissionDenied : CommandIntro
}
