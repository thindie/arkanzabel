package com.thindie.rknzbl.appfeatures.settings.ui.source

import androidx.compose.runtime.Immutable
import com.thindie.engine.core.ViewState

@Immutable
internal data class SourceState(
  val currentUrl: String? = null,
  val customUrlInput: String = "",
) : ViewState
