package com.thindie.rknzbl.appfeatures.home.ui

import androidx.compose.runtime.Immutable
import com.thindie.engine.core.ViewState
import com.v2ray.ang.dto.ConnectionProfile

@Immutable
internal data class ScreenState(
  val connectedProfile: ConnectionProfile? = null,
  val profilesLoading: Boolean = false,
  val serviceConnection: Boolean = false,
) : ViewState
