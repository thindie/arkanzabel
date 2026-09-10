package com.v2ray.ang.runtime

import android.content.Context
import com.thindie.engine.core.Log
import com.v2ray.ang.AppConfig
import com.v2ray.ang.util.Utils
import go.Seq
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import libv2ray.Libv2ray
import java.util.concurrent.atomic.AtomicBoolean

/**
 * V2Ray Native Library Manager
 *
 * Thread-safe singleton wrapper for Libv2ray native methods.
 * Provides initialization protection and unified API for V2Ray core operations.
 */
object V2RayNativeManager {
  private val initialized = AtomicBoolean(false)

  /**
   * Initialize V2Ray core environment.
   * This method is thread-safe and ensures initialization happens only once.
   * Subsequent calls will be ignored silently.
   *
   */
  fun initCoreEnv(context: Context?) {
    if (initialized.compareAndSet(false, true)) {
      try {
        Seq.setContext(context?.applicationContext)
        val assetPath = Utils.userAssetPath(context)
        val deviceId = Utils.getDeviceIdForXUDPBaseKey()
        Libv2ray.initCoreEnv(assetPath, deviceId)
        Log.i({ "V2Ray core environment initialized successfully" }, AppConfig.TAG)
      } catch (runtime: RuntimeException) {
        Log.e({ "Failed to initialize V2Ray core environment" }, AppConfig.TAG, runtime)
        initialized.set(false)
        throw runtime
      }
    } else {
      Log.d({ "V2Ray core environment already initialized, skipping" }, AppConfig.TAG)
    }
  }

  /**
   * Get V2Ray core version.
   *
   * @return Version string of the V2Ray core
   */
  fun getLibVersion(): String {
    return try {
      Libv2ray.checkVersionX()
    } catch (runtime: RuntimeException) {
      Log.e({ "Failed to check V2Ray version" }, AppConfig.TAG, runtime)
      "Unknown"
    }
  }

  /**
   * Measure outbound connection delay.
   *
   * @param config The configuration JSON string
   * @param testUrl The URL to test against
   * @return Delay in milliseconds, or -1 if test failed
   */
  fun measureOutboundDelay(
    config: String,
    testUrl: String,
  ): Long {
    return try {
      Libv2ray.measureOutboundDelay(config, testUrl)
    } catch (runtime: Exception) {
      Log.e({ "Failed to measure outbound delay" }, AppConfig.TAG, runtime)
      -1L
    }
  }

  /**
   * Create a new core controller instance.
   *
   * @param handler The callback handler for core events
   * @return A new CoreController instance
   */
  fun newCoreController(handler: CoreCallbackHandler): CoreController {
    return try {
      Libv2ray.newCoreController(handler)
    } catch (runtime: RuntimeException) {
      Log.e({ "Failed to create core controller" }, AppConfig.TAG, runtime)
      throw runtime
    }
  }
}
