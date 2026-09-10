package com.v2ray.ang.testutil

import android.text.TextUtils
import android.util.Base64
import io.mockk.every
import io.mockk.mockkStatic
import java.util.Base64 as JavaBase64

/**
 * These parsers run under plain JVM unit tests (no Robolectric), but touch a handful of
 * Android framework statics whose stub jar throws "not mocked" on any real call. Stub the
 * few methods actually exercised by the protocol parsers so their behavior can be
 * characterized without pulling in an instrumented test runner.
 */
fun mockAndroidFrameworkStatics() {
  // com.thindie.engine.core.Log is JVM-safe (println + sinks), no stub needed.
  mockkStatic(TextUtils::class)
  every { TextUtils.isEmpty(any()) } answers { firstArg<CharSequence?>().isNullOrEmpty() }

  mockkStatic(Base64::class)
  every { Base64.decode(any<String>(), any()) } answers {
    val text = firstArg<String>()
    val flags = secondArg<Int>()
    val urlSafe = flags and 8 != 0 // android.util.Base64.URL_SAFE
    val padded = if (text.length % 4 == 0) text else text + "=".repeat(4 - text.length % 4)
    if (urlSafe) JavaBase64.getUrlDecoder().decode(padded) else JavaBase64.getDecoder().decode(padded)
  }
  every { Base64.encodeToString(any<ByteArray>(), any()) } answers {
    JavaBase64.getEncoder().encodeToString(firstArg())
  }
}
