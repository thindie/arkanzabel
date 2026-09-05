package com.thindie.rknzbl.appfeatures.settings.domain

import androidx.compose.runtime.Immutable

@Immutable
data class AppRow(
  val appName: String,
  val packageName: String,
)
