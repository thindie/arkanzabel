package com.thindie.rknzbl.appfeatures.home.di

import android.content.Context
import com.thindie.rknzbl.application.work.GlobalJobManager
import com.thindie.rknzbl.appversion.AppVersionResolver
import com.thindie.rknzbl.domain.ConnectionProfileRepository

/**
 * One flow == one module.
 *
 * Owns the feature-specific dependencies for the [com.thindie.rknzbl.appfeatures.home.HomeFlow]:
 * the shared profile repository (which encapsulates VPN service operations), the app-level
 * [GlobalJobManager] used to run long jobs that must survive navigation, the [context] needed to
 * open external links, and the [appVersionResolver] that drives the update prompt.
 * Created once in [com.thindie.rknzbl.application.di.ApplicationScope] and injected into the flow.
 */
class HomeFlowModule(
  val connectionProfileRepository: ConnectionProfileRepository,
  val globalJobManager: GlobalJobManager,
  val context: Context,
  val appVersionResolver: AppVersionResolver,
)
