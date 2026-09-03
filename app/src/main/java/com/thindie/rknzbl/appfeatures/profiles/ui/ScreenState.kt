package com.thindie.rknzbl.appfeatures.profiles.ui

import androidx.compose.runtime.Immutable
import com.thindie.engine.core.ViewState
import com.thindie.engine.core.WorkState
import com.v2ray.ang.dto.ConnectionProfile

@Immutable
/** Tab index: 0 = Main, 1 = Saved */
data class ScreenState(
  val selectedTab: Int = 0,
  val profiles: List<ConnectionProfile> = emptyList(),
  val savedProfiles: List<ConnectionProfile> = emptyList(),
  val pingResults: Map<String, Long> = emptyMap(),
  val connectedProfile: ConnectionProfile? = null,
  val workState: WorkState = WorkState.Idle,
  val connectingSince: Long? = null,
) : ViewState
