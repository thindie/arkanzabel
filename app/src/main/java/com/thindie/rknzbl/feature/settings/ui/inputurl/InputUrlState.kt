package com.thindie.rknzbl.feature.settings.ui.inputurl

import androidx.compose.runtime.Immutable
import com.thindie.engine.core.ViewState

@Immutable
internal data class InputUrlState(
  val url: String = "",
) : ViewState
