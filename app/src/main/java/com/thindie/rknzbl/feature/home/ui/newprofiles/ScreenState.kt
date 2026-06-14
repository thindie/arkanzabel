package com.thindie.rknzbl.feature.home.ui.newprofiles

import androidx.compose.runtime.Immutable
import com.thindie.rknzbl.engine.State
import com.v2ray.ang.dto.ConnectionProfile

@Immutable
data class ScreenState(
  val sourceName: String,
  val sourceUrl: String,
  val links: List<ConnectionProfile> = emptyList(),
  val selected: ConnectionProfile? = null,
  val established: Boolean = false,
  val serviceBeingStarted: Boolean? = null,
) : State
