package com.thindie.rknzbl.appfeatures.home.di

import com.thindie.rknzbl.feature.home.domain.ConnectionProfileRepository

/**
 * One flow == one module.
 *
 * Owns the feature-specific dependencies for the [com.thindie.rknzbl.appfeatures.home.HomeFlow]:
 * the shared profile repository (which encapsulates VPN service operations).
 * Created once in [com.thindie.rknzbl.application.di.ApplicationScope] and injected into the flow.
 */
class HomeFlowModule(
  val connectionProfileRepository: ConnectionProfileRepository,
)
