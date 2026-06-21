package com.thindie.rknzbl.feature.settings.ui

import androidx.compose.runtime.Immutable
import com.thindie.rknzbl.engine.State

@Immutable
internal data class ScreenState(
  val autosaveEnabled: Boolean? = null,
  val muxEnabled: Boolean? = null,
  val isLocalSave: Boolean? = null,
  val language: String? = null,
  val legacyRestart: Boolean = false,
) : State
