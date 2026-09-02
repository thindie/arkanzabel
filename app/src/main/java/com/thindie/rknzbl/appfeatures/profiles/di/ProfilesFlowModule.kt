package com.thindie.rknzbl.appfeatures.profiles.di

import com.thindie.rknzbl.feature.home.domain.ConnectionProfileRepository

/**
 * One flow == one module.
 *
 * Owns the feature-specific dependencies for the [com.thindie.rknzbl.appfeatures.profiles.ProfilesFlow].
 * Created once in [com.thindie.rknzbl.application.di.ApplicationScope] and injected into the flow.
 */
class ProfilesFlowModule(
  val connectionProfileRepository: ConnectionProfileRepository,
)
