package com.thindie.rknzbl.appfeatures.settings.ui.perapp

import androidx.compose.runtime.Immutable
import com.thindie.engine.core.ViewState
import com.thindie.rknzbl.appfeatures.settings.domain.AppRow

@Immutable
internal data class PerAppViewState(
  val mode: ProxyScopeMode,
  val allApps: List<AppRow>,
  val selectedPackages: Set<String>,
) : ViewState

enum class ProxyScopeMode {
  All,
  Selected,
}
