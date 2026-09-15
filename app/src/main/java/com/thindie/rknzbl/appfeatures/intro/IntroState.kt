package com.thindie.rknzbl.appfeatures.intro

import androidx.compose.runtime.Immutable
import com.thindie.engine.core.ViewState

@Immutable
data class ViewState(
  val permissionScope: List<Permission> =
    buildList {
      add(Permission.Vpn)
      add(Permission.Push)
    },
  val permit: List<Permission> = emptyList(),
  val current: Permission = Permission.Vpn,
  val stage: Stage = Stage.Loading,
  val hint: String? = null,
) : ViewState

enum class Stage {
  Loading,
  SoftRequest,
  Rationale,
  RationaleDismissedOnce,
}

enum class Permission {
  Vpn,
  Push,
}
