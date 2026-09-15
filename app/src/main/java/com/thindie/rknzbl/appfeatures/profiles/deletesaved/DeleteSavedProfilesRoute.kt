package com.thindie.rknzbl.appfeatures.profiles.deletesaved

import com.thindie.engine.core.RouteFactory
import com.thindie.rknzbl.appfeatures.profiles.ProfilesFlow

internal fun ProfilesFlow.deleteSavedProfiles() =
  RouteFactory.create(
    initialState = DeleteSavedProfilesState(isLocalSave = flowModule.connectionProfileRepository.isLocalStorage()),
    execute = { c, s ->
      exec(
        c = c,
        s = s,
        connectionProfileRepository = flowModule.connectionProfileRepository,
        globalJobManager = flowModule.globalJobManager,
        onFinish = { back() },
      )
    },
    stateSink = { screenScope -> stateSink(flowModule.connectionProfileRepository, screenScope) },
    routeContent = ::DeleteProfilesScreen,
    initialCommand = { DeleteSavedProfilesCommand.LoadSaved },
    id = "ProfilesFlow-deleted-saved",
  )
