package com.thindie.rknzbl.feature.managegate.storedgates.profiles

import com.thindie.rknzbl.engine.RouteFactory
import com.thindie.rknzbl.feature.managegate.storedgates.FavoriteProfilesFlow

internal fun FavoriteProfilesFlow.profiles() =
  RouteFactory.create(
    initialState = ScreenState(),
    execute = ::exec,
    stateSink = ::stateSink,
    routeContent = { ProfilesScreen() },
    errorMapper = ::errorMapper,
    initialCommand = { ScreenCommand.RequestStoredProfiles },
    id = "profiles",
  )
