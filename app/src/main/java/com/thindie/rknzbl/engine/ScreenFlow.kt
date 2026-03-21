package com.thindie.rknzbl.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

abstract class ScreenFlow<R : Route, RESULT>(private val router: Router) {
  // known issue: the first route added not to 'ids'. temp resolution: additional router.pop()
  private val ids = MutableStateFlow(listOf<Route.Id>())

  private var finish: RESULT? = null

  fun finish(r: RESULT) {
    if (ids.value.isEmpty()) {
      router.pop()
      finish = r
    } else {
      router.removeAll(ids.value.toSet())
      router.pop()
      finish = r
    }
  }

  fun go(route: R) {
    if (route.id in ids.value) {
      val oldIds = ids.value
      val newIds = oldIds.dropLastWhile { it != route.id }
      val removeIds = (oldIds - newIds).toSet()
      router.removeAll(removeIds)
    } else {
      ids.update { it + route.id }
      router.push(route)
    }
  }

  fun back() {
    val last = ids.value.lastOrNull()
    if (last != null) {
      ids.value = ids.value.dropLast(1)
      router.removeAll(setOf(last))
    } else {
      finish = null
      router.pop()
    }
  }

  abstract fun start()

  fun onFinishBuilder(action: (RESULT) -> Unit): ScreenFlow<R, RESULT> {
    val result = this.finish
    this.finish = null
    result?.let { action(it) }
    return this
  }
}