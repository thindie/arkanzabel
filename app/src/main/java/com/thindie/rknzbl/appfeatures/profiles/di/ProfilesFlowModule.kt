package com.thindie.rknzbl.appfeatures.profiles.di

import com.thindie.rknzbl.application.work.GlobalJobManager
import com.thindie.rknzbl.domain.ConnectionProfileRepository

/**
 * One flow == one module.
 *
 * Owns the feature-specific dependencies for the [com.thindie.rknzbl.appfeatures.profiles.ProfilesFlow]:
 * the shared profile repository and the app-level [GlobalJobManager] used to run long jobs that
 * must survive navigation.
 * Created once in [com.thindie.rknzbl.application.di.ApplicationScope] and injected into the flow.
 */
class ProfilesFlowModule(
  val connectionProfileRepository: ConnectionProfileRepository,
  val globalJobManager: GlobalJobManager,
)
