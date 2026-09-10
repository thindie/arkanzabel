package com.thindie.rknzbl.appfeatures.profiles.deletesaved

import com.thindie.engine.core.ScreenScope
import com.thindie.engine.core.sub
import com.thindie.engine.core.transition
import com.thindie.rknzbl.domain.ConnectionProfileRepository
import kotlinx.coroutines.flow.filterNotNull

internal fun stateSink(
  connectionProfileRepository: ConnectionProfileRepository,
  screenScope: ScreenScope<DeleteSavedProfilesState, DeleteSavedProfilesCommand>,
) {
  screenScope.sub(connectionProfileRepository.stored.filterNotNull()).transition { state, saved ->
    state.copy(savedProfiles = saved)
  }

  screenScope.sub(connectionProfileRepository.connected).transition { state, connected ->
    state.copy(connectedProfile = connected)
  }
}
