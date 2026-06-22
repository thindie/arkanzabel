package com.thindie.rknzbl.feature.settings.ui.inputurl

import androidx.compose.runtime.Immutable
import com.thindie.rknzbl.engine.State

@Immutable
internal data class InputUrlState(
  val url: String = "",
) : State
