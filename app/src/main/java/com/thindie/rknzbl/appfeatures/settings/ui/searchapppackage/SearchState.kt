package com.thindie.rknzbl.appfeatures.settings.ui.searchapppackage

import androidx.compose.runtime.Immutable
import com.thindie.engine.core.ViewState
import com.thindie.rknzbl.appfeatures.settings.domain.AppRow

@Immutable
internal data class SearchState(
  val searchQuery: String,
  val allApps: List<AppRow>,
  val selectedPackages: Set<String>,
) : ViewState
