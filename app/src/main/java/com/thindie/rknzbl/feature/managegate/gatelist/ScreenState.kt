package com.thindie.rknzbl.feature.managegate.gatelist

import androidx.compose.runtime.Immutable

@Immutable
data class ScreenState(
  val selected: SelectSourceFlow.Result = SelectSourceFlow.Result.NotSelected,
  val blackSection: List<SelectSourceFlow.Result> =
    listOf(
      SelectSourceFlow.Result.FullBlackShadowSocks,
      SelectSourceFlow.Result.FullBlackVless,
      SelectSourceFlow.Result.MobileBlackVless,
      SelectSourceFlow.Result.StoredProfiles,
    ),
  val whiteSection: List<SelectSourceFlow.Result> =
    listOf(
      SelectSourceFlow.Result.WhiteListMobile,
      SelectSourceFlow.Result.WhiteListMobileV2,
      SelectSourceFlow.Result.WhiteListAll,
      SelectSourceFlow.Result.WhiteListRussian,
    ),
) : com.thindie.rknzbl.engine.State
