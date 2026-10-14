package com.thindie.rknzbl.appfeatures.settings.ui

import androidx.compose.runtime.Immutable
import com.thindie.engine.core.Section
import com.thindie.engine.core.ViewState
import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.WebDavConfig

@Immutable
internal data class ScreenState(
  val section: Section = Section.Leaf,
  val autosaveEnabled: Boolean? = null,
  val muxEnabled: Boolean? = null,
  val isLocalSave: Boolean? = null,
  val language: String? = null,
  val legacyRestart: Boolean = false,
  val speedEnabled: Boolean? = null,
  // Presence of a non-blank URL means the custom source is active
  val customSourceUrl: String? = null,
  // WebDAV storage config; null means the endpoint is not configured
  val webDavConfig: WebDavConfig? = null,
  // When true, built-in server settings are used and any stored manual config is ignored for display
  val webDavUseDefaults: Boolean = false,
  // Number of profiles pinged in parallel during measurement (settings feature)
  val parallelPingCount: Int = AppConfig.DEFAULT_PARALLEL_PING_COUNT,
  // Number of reachable profiles checked per ASAP cycle, i.e. the series length (settings feature)
  val asapSeriesSize: Int = AppConfig.DEFAULT_ASAP_SERIES_SIZE,
) : ViewState
