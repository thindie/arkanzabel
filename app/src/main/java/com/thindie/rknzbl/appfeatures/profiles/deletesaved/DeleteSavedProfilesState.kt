package com.thindie.rknzbl.appfeatures.profiles.deletesaved

import androidx.compose.runtime.Immutable
import com.thindie.engine.core.ViewState
import com.v2ray.ang.dto.ConnectionProfile

@Immutable
data class DeleteSavedProfilesState(
  val savedProfiles: List<ConnectionProfile> = emptyList(),
  val selectedProfiles: Set<ConnectionProfile> = emptySet(),
  val connectedProfile: ConnectionProfile? = null,
) : ViewState
