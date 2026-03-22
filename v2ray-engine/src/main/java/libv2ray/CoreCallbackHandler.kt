package libv2ray

/** Stub matching gomobile-generated shape; replace when shipping real `libv2ray`. */
interface CoreCallbackHandler {
  fun startup(): Long

  fun shutdown(): Long

  fun onEmitStatus(l: Long, s: String?): Long
}
