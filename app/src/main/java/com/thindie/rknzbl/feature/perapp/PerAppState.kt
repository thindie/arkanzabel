package com.thindie.rknzbl.feature.perapp

import androidx.compose.runtime.Immutable

@Immutable
data class AppRow(
  val appName: String,
  val packageName: String,
)

@Immutable
data class ViewState(
  val mode: ProxyScopeMode,
  val allApps: List<AppRow>,
  val selectedPackages: Set<String>,
) : com.thindie.engine.core.ViewState

@Immutable
data class SearchState(
  val searchQuery: String,
  val allApps: List<AppRow>,
  val selectedPackages: Set<String>,
) : com.thindie.engine.core.ViewState
