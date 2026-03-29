package com.v2ray.ang.extension

import kotlinx.coroutines.CancellationException

suspend inline fun <T> runSuspendCatching(
  crossinline block: suspend () -> T,
  crossinline onError: (RuntimeException) -> T,
): T {
  return try {
    block()
  } catch (cancel: CancellationException) {
    throw cancel
  } catch (runtime: RuntimeException) {
    onError(runtime)
  }
}
