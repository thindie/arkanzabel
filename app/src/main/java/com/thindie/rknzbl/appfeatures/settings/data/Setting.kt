package com.thindie.rknzbl.appfeatures.settings.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A single persisted setting backed by a [MutableStateFlow] plus a
 * read/write pair over [KeyValueStorage].
 *
 * The public [flow] is the source of truth for the UI (reactive), while
 * [set] writes to both the in-memory flow and storage atomically, so a
 * change is never lost when no subscriber is attached and is never
 * double-written through a separate persistence path.
 *
 * [read] and [write] bridge the typed value to/from storage. The default
 * implementations store everything as a string, which is sufficient for
 * the settings in this feature; override [read]/[write] when a setting
 * needs typed storage (e.g. booleans or ints).
 */
class Setting<T>(
  private val read: () -> T,
  private val write: (T) -> Unit,
) {
  private val value = MutableStateFlow(read())

  /**
   * The current value, exposed reactively to the UI.
   */
  val flow: Flow<T> = value.asStateFlow()

  /**
   * The current value, read synchronously from memory (no collector needed).
   */
  fun getSync(): T = value.value

  /**
   * Update the setting in memory and persist it.
   */
  fun set(newValue: T) {
    value.value = newValue
    write(newValue)
  }
}
