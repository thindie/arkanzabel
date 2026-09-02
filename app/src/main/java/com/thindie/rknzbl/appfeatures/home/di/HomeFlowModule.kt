package com.thindie.rknzbl.appfeatures.home.di

import android.content.Context
import com.thindie.rknzbl.feature.home.domain.ConnectionProfileRepository

/**
 * One flow == one module.
 *
 * Owns the feature-specific dependencies for the [com.thindie.rknzbl.appfeatures.home.HomeFlow]:
 * the shared profile repository, the legacy settings repository (needed to launch stored-profiles)
 * and the application context (used by the legacy flows it drives).
 * Created once in [com.thindie.rknzbl.application.di.ApplicationScope] and injected into the flow.
 */
class HomeFlowModule(
  val connectionProfileRepository: ConnectionProfileRepository,
  val appContext: Context,
)
