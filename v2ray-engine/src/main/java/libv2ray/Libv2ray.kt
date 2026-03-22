package libv2ray

/** Stub until `libv2ray` AAR is placed under `v2ray-engine/libs/`. */
object Libv2ray {
  @JvmStatic
  fun initCoreEnv(assetPath: String, deviceId: String) {}

  @JvmStatic
  fun newCoreController(handler: CoreCallbackHandler): CoreController = CoreController()

  @JvmStatic
  fun checkVersionX(): String = "stub"

  @JvmStatic
  fun measureOutboundDelay(config: String, testUrl: String): Long = -1L
}
