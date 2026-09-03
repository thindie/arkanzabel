package com.thindie.rknzbl.appfeatures.home.ui

import androidx.compose.runtime.Immutable
import com.thindie.engine.core.ViewState
import com.thindie.engine.core.WorkState
import com.v2ray.ang.dto.ConnectionProfile

@Immutable
internal data class ScreenState(
  val workState: WorkState = WorkState.Idle,
  val connectedProfile: ConnectionProfile? = null,
  /** Timestamp of the last connect attempt; set while waiting for service confirmation. */
  val connectingSince: Long? = null,
) : ViewState
