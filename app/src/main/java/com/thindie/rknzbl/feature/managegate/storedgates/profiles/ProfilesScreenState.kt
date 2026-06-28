package com.thindie.rknzbl.feature.managegate.storedgates.profiles

import androidx.compose.runtime.Immutable
import com.thindie.rknzbl.engine.State
import com.v2ray.ang.dto.ConnectionProfile
import com.v2ray.ang.runtime.SpeedtestManager

@Immutable
data class ScreenState(
  val profiles: List<ConnectionProfile> = emptyList(),
  val selected: ConnectionProfile? = null,
  val selectedTestConnectionMessage: SpeedtestManager.SpeedTestResult? = null,
  val selectedProfiles: Set<ConnectionProfile> = emptySet(),
  val selectionMode: Boolean = false,
  val isLocalMode: Boolean = false,
) : State
