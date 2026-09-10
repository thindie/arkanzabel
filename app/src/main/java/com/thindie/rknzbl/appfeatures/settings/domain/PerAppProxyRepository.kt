package com.thindie.rknzbl.appfeatures.settings.domain

import kotlinx.coroutines.flow.Flow

/**
 * Per-app proxy settings (new design). Backed by the same storage keys as the legacy flow,
 * so both designs share one source of truth.
 */
interface PerAppProxyRepository {
  /** True when only selected apps are routed through the VPN. */
  val selectedOnly: Flow<Boolean>

  suspend fun setMode(selectedOnly: Boolean)

  val packages: Flow<Set<String>>

  suspend fun setPackages(packages: Set<String>)

  /** Installed apps eligible for per-app tunneling, sorted by name. */
  suspend fun loadInstalledApps(): List<AppRow>
}
