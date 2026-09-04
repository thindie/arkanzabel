package com.thindie.engine.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Reactive cache backed by MutableStateFlow.
 * Provides get/set/clear/invalidate operations with observable state changes.
 */
class Cache<T>(initial: T?) {
  private val _value = MutableStateFlow(initial)

  /** Observable value of the cached item. */
  val value: StateFlow<T?> = _value.asStateFlow()

  /** Current cached value, or null if empty. */
  fun get(): T? = _value.value

  /** Store a value in the cache. Triggers state emission. */
  fun set(item: T) {
    _value.value = item
  }

  /** Update the cached value using a transform function. */
  fun updateValue(transform: (T?) -> T?) {
    _value.update { current -> transform(current) }
  }

  /** Clear the cache, setting it to null. Triggers state emission. */
  fun clear() {
    _value.value = null
  }

  /** Check if the cache contains a value. */
  fun isEmpty(): Boolean = get() == null

  /** Invalidate and replace with a new computed value. */
  fun invalidateAndSet(item: T) {
    set(item)
  }
}
