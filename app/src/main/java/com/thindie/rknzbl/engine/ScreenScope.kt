package com.thindie.rknzbl.engine

import androidx.compose.runtime.Stable


@Stable
interface ScreenScope<S : State, C : Command> {
  val state: androidx.compose.runtime.State<S>
  val processing: androidx.compose.runtime.State<C?>
  val error: androidx.compose.runtime.State<ScreenScopeError?>
  fun send(command: C)
  fun dispose()
}
