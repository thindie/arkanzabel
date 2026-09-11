package com.thindie.rknzbl.appfeatures.settings.ui

import androidx.compose.runtime.Immutable
import com.thindie.engine.core.Section
import com.thindie.engine.core.ViewState
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
  val useNewDesign: Boolean? = null,
  // Presence of a non-blank URL means the custom source is active
  val customSourceUrl: String? = null,
  // WebDAV storage config; null means the endpoint is not configured
  val webDavConfig: WebDavConfig? = null,
) : ViewState
