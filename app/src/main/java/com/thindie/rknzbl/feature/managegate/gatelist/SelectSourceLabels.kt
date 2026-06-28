package com.thindie.rknzbl.feature.managegate.gatelist

import android.content.Context
import com.thindie.rknzbl.R

internal fun SelectSourceFlow.Result.resolveLabels(context: Context): Pair<String, String> =
  when (this) {
    SelectSourceFlow.Result.FullBlackShadowSocks ->
      context.getString(R.string.source_black_ss_title) to
        context.getString(R.string.source_subtitle_all_configs)

    SelectSourceFlow.Result.FullBlackVless,
    SelectSourceFlow.Result.NotSelected,
    ->
      context.getString(R.string.source_black_vless_title) to
        context.getString(R.string.source_subtitle_all_configs)

    SelectSourceFlow.Result.MobileBlackVless ->
      context.getString(R.string.source_black_vless_title) to
        context.getString(R.string.source_subtitle_top150_phone)

    SelectSourceFlow.Result.WhiteListAll ->
      context.getString(R.string.source_white_cidr_title) to
        context.getString(R.string.source_subtitle_all_configs)

    SelectSourceFlow.Result.WhiteListMobile ->
      context.getString(R.string.source_white_cidr_title) to
        context.getString(R.string.source_subtitle_top150_phone_1)

    SelectSourceFlow.Result.WhiteListMobileV2 ->
      context.getString(R.string.source_white_cidr_title) to
        context.getString(R.string.source_subtitle_top150_phone_2)

    SelectSourceFlow.Result.WhiteListRussian ->
      context.getString(R.string.source_white_cidr_title) to
        context.getString(R.string.source_subtitle_ru_services)

    SelectSourceFlow.Result.StoredProfiles ->
      context.getString(R.string.source_stored) to
        context.getString(R.string.source_subtitle_stored)

    is SelectSourceFlow.Result.CustomSource ->
      context.getString(R.string.source_custom_title) to
        context.getString(R.string.source_subtitle_custom)
  }
