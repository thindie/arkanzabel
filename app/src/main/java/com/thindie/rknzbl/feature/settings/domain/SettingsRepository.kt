package com.thindie.rknzbl.feature.settings.domain

import com.thindie.engine.uikit.ThemeSwitcher
import com.thindie.rknzbl.feature.settings.ui.SniffingPortRange
import com.thindie.rknzbl.feature.settings.ui.SniffingTarget
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Settings feature.
 *
 * Boolean options expose a reactive [Flow] plus a `toggle*` suspend function (write to the private
 * MutableStateFlow; the flow's `onEach` persists the value). String/enum options expose either a
 * single [Flow] with a setter, or a getter-flow + setter, matching how they are consumed.
 */
interface SettingsRepository {
  /** Reactive flow for autosave state updates */
  val autosaveEnabled: Flow<Boolean>

  /** Enable or disable autosave */
  suspend fun toggleAutosave(enabled: Boolean)

  val themeChoice: Flow<ThemeSwitcher.Choice>

  // MUX support
  val muxEnabled: Flow<Boolean>

  suspend fun toggleMux(enabled: Boolean)

  // Fragment support (Recommendation #5 — global packet fragmentation, applied by OutboundConfigStep on TLS/REALITY outbounds)

  val fragmentEnabled: Flow<Boolean>

  suspend fun toggleFragment(enabled: Boolean)

  fun setFragmentLength(length: String)

  val fragmentInterval: Flow<String?>

  fun setFragmentInterval(interval: String)

  // Local storage mode support

  val isLocalSave: Flow<Boolean>

  suspend fun toggleLocalSave(enabled: Boolean)

  val startWithFavoriteProfiles: Flow<Boolean>

  suspend fun toggleStartWithFavoriteProfiles(enabled: Boolean)

  // Speed notification

  val speedEnabled: Flow<Boolean?>

  suspend fun toggleSpeed(enabled: Boolean)

  // Language - read from storage on subscription start
  fun getLanguageSync(): String?

  val language: Flow<String?>

  fun setLanguage(code: String)

  // Custom source URL

  val customSourceUrl: Flow<String?>

  fun setCustomSourceUrl(url: String)

  val isCustomSourceEnabled: Flow<Boolean>

  fun setCustomSourceEnabled(enabled: Boolean)

  val forceProfileMeasure: Flow<Boolean>
  val getForceProfileMeasureSync: Boolean

  fun setForceProfileMeasure(enabled: Boolean)

  // Reality masquerade (show) support — global toggle for TLS/REALITY outbounds

  val realityShowEnabled: Flow<Boolean>

  suspend fun toggleRealityShow(enabled: Boolean)

  // Sniffing target protocol support
  fun sniffingTarget(): Flow<SniffingTarget?>

  fun setSniffingTarget(target: SniffingTarget)

  // Sniffing port-range support
  fun sniffingPortRange(): Flow<SniffingPortRange?>

  fun setSniffingPortRange(range: SniffingPortRange)
}
