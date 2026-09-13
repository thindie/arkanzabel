package com.thindie.rknzbl.appfeatures.settings.ui.webdav

import androidx.compose.runtime.Immutable
import com.thindie.engine.core.ViewState

@Immutable
internal data class WebdavState(
  val urlInput: String = "",
  val usernameInput: String = "",
  val passwordInput: String = "",
  val useDefaults: Boolean = false,
) : ViewState
