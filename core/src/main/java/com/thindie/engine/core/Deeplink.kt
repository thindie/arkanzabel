package com.thindie.engine.core

sealed interface Deeplink {
  data object Main : Deeplink

  data object NotSpecified : Deeplink
}
