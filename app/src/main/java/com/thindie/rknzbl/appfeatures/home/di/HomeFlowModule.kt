package com.thindie.rknzbl.appfeatures.home.di

import com.thindie.rknzbl.application.work.GlobalJobManager
import com.thindie.rknzbl.feature.home.domain.ConnectionProfileRepository

/**
 * One flow == one module.
 *
 * Owns the feature-specific dependencies for the [com.thindie.rknzbl.appfeatures.home.HomeFlow]:
 * the shared profile repository (which encapsulates VPN service operations) and the app-level
 * [GlobalJobManager] used to run long jobs that must survive navigation.
 * Created once in [com.thindie.rknzbl.application.di.ApplicationScope] and injected into the flow.
 */
class HomeFlowModule(
  val connectionProfileRepository: ConnectionProfileRepository,
  val globalJobManager: GlobalJobManager,
)
