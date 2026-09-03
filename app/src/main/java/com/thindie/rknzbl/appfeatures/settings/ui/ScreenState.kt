package com.thindie.rknzbl.appfeatures.settings.ui

import androidx.compose.runtime.Immutable
import com.thindie.engine.core.ViewState

@Immutable
internal data class ScreenState(
  val section: com.thindie.engine.core.Section = com.thindie.engine.core.Section.Leaf,
  val autosaveEnabled: Boolean? = null,
  val muxEnabled: Boolean? = null,
  val isLocalSave: Boolean? = null,
  val language: String? = null,
  val legacyRestart: Boolean = false,
  val speedEnabled: Boolean? = null,
  val useNewDesign: Boolean? = null,
  val isCustomSourceEnabled: Boolean = false,
  val customSourceUrl: String? = null,
) : ViewState
