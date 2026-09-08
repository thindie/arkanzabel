package com.thindie.rknzbl.appfeatures.home.ui

import androidx.compose.runtime.Immutable
import com.thindie.engine.core.ViewState
import com.v2ray.ang.dto.ConnectionProfile

@Immutable
internal data class ScreenState(
  val connectedProfile: ConnectionProfile? = null,
  // Best profile of the last completed ping batch; enables reconnect without re-measuring.
  val lastBestProfile: ConnectionProfile? = null,
  val profilesLoading: Boolean = false,
  val hasProfiles: Boolean = false,
  val screenVpnState: ScreenVpnState = ScreenVpnState.NotStarted,
  val vpnError: String? = null,
) : ViewState

enum class ScreenVpnState {
  NotStarted,
  TurningOff,
  TurningOn,
  Running,
  Error,
}
