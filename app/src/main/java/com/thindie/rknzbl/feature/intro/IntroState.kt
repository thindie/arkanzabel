package com.thindie.rknzbl.feature.intro

import androidx.compose.runtime.Immutable

@Immutable
data class State(
  val permissionScope: List<Permission> =
    buildList {
      add(Permission.Vpn)
      add(Permission.Push)
    },
  val permit: List<Permission> = emptyList(),
  val current: Permission = Permission.Vpn,
  val stage: Stage = Stage.Loading,
  val hint: String? = null,
) : com.thindie.rknzbl.engine.State

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
