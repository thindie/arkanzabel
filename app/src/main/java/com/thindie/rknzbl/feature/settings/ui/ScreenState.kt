package com.thindie.rknzbl.feature.settings.ui

import androidx.compose.runtime.Immutable
import com.thindie.rknzbl.engine.State
import com.thindie.rknzbl.uikit.ThemeSwitcher

/**
 * Immutable state for the Settings Screen (MVI Pattern).
 * Contains theme mode and autosave settings.
 */
@Immutable
internal data class SettingsScreenState(
  val autosaveEnabled: Boolean? = null,
) : State
