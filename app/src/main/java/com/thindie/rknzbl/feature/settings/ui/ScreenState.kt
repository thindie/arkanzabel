package com.thindie.rknzbl.feature.settings.ui

import androidx.compose.runtime.Immutable
import com.thindie.engine.core.ViewState

@Immutable
internal data class ScreenState(
  val autosaveEnabled: Boolean? = null,
  val muxEnabled: Boolean? = null,
  val fragmentEnabled: Boolean? = null,
  val fragmentInterval: String? = null,
  val isLocalSave: Boolean? = null,
  val language: String? = null,
  val legacyRestart: Boolean = false,
  val startWithFavoriteProfiles: Boolean? = null,
  val speedEnabled: Boolean? = null,
  val customSourceUrl: String? = null,
  val isCustomSourceEnabled: Boolean = false,
  val forceProfileMeasure: Boolean? = null,
  val realityShowEnabled: Boolean? = null,
  val sniffingTarget: SniffingTarget = SniffingTarget.All,
  val sniffingPortRange: SniffingPortRange = SniffingPortRange.All,
) : ViewState
