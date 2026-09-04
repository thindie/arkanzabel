package com.thindie.rknzbl.feature.home.ui.newprofiles

import androidx.compose.runtime.Immutable
import com.thindie.engine.core.ViewState
import com.thindie.engine.core.WorkState
import com.v2ray.ang.dto.ConnectionProfile
import com.v2ray.ang.runtime.SpeedtestManager

@Immutable
data class ScreenState(
  val sourceName: String,
  val sourceUrl: String,
  val links: List<ConnectionProfile> = emptyList(),
  val selected: ConnectionProfile? = null,
  val selectedTestConnectionMessage: SpeedtestManager.SpeedTestResult? = null,
  val pingResults: Map<ConnectionProfile, Long>? = null,
  val filter: FilterMode = FilterMode.All,
  val pingState: WorkState = WorkState.Idle,
  val refreshing: Boolean = false,
  val sourceAvailable: Boolean? = null,
) : ViewState
