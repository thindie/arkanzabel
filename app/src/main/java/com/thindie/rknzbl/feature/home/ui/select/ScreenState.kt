package com.thindie.rknzbl.feature.home.ui.select

import com.v2ray.ang.dto.ConnectionProfile

internal data class ScreenState(val autoSaved: ConnectionProfile? = null) :
  com.thindie.rknzbl.engine.State
