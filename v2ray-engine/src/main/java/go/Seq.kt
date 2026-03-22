package go

import android.content.Context

/**
 * gomobile stub until real `libv2ray` AAR is linked. Real implementation sets JNI context.
 */
object Seq {
  @JvmStatic
  fun setContext(ctx: Context?) {}
}
