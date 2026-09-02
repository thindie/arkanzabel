package com.thindie.rknzbl.appfeatures.settings.ui

import androidx.compose.runtime.Immutable
import com.thindie.engine.core.ViewState

@Immutable
internal data class ScreenState(
  val autosaveEnabled: Boolean? = null,
  val muxEnabled: Boolean? = null,
  val isLocalSave: Boolean? = null,
  val language: String? = null,
  val legacyRestart: Boolean = false,
  val speedEnabled: Boolean? = null,
  val useNewDesign: Boolean? = null,
) : ViewState
