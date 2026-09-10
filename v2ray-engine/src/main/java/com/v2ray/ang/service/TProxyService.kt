package com.v2ray.ang.service

import android.content.Context
import android.os.ParcelFileDescriptor
import com.thindie.engine.core.Log
import com.v2ray.ang.AppConfig
import com.v2ray.ang.contracts.Tun2SocksControl
import com.v2ray.ang.runtime.KeyValueStorage
import com.v2ray.ang.runtime.SettingsManager
import java.io.File

/**
 * Manages the tun2socks process that handles VPN traffic
 */
class TProxyService(
  private val context: Context,
  private val vpnInterface: ParcelFileDescriptor,
  private val isRunningProvider: () -> Boolean,
  private val restartCallback: () -> Unit,
) : Tun2SocksControl {
  companion object {
    @JvmStatic
    @Suppress("FunctionName")
    private external fun TProxyStartService(
      configPath: String,
      fd: Int,
    )

    @JvmStatic
    @Suppress("FunctionName")
    private external fun TProxyStopService()

    @JvmStatic
    @Suppress("FunctionName")
    private external fun TProxyGetStats(): LongArray?

    init {
      System.loadLibrary("hev-socks5-tunnel")
    }
  }

  /**
   * Starts the tun2socks process with the appropriate parameters.
   */
  override fun startTun2Socks() {
//        Log.i({ "Starting HevSocks5Tunnel via JNI" }, AppConfig.TAG)

    val configContent = buildConfig()
    val configFile =
      File(context.filesDir, "hev-socks5-tunnel.yaml").apply {
        writeText(configContent)
        // 600: owner read/write only — config contains network settings
        setReadable(true, false)
        setReadable(false, true)
        setWritable(true, false)
        setWritable(false, true)
        setExecutable(false, true)
      }
//        Log.i({ "Config file created: ${configFile.absolutePath}" }, AppConfig.TAG)

    try {
//            Log.i({ "TProxyStartService..." }, AppConfig.TAG)
      TProxyStartService(configFile.absolutePath, vpnInterface.fd)
    } catch (e: Exception) {
      Log.e({ "HevSocks5Tunnel exception: ${e.message}" }, AppConfig.TAG)
    }
  }

  private fun buildConfig(): String {
    val socksPort = SettingsManager.getSocksPort()
    val vpnConfig = SettingsManager.getCurrentVpnInterfaceAddressConfig()
    return buildString {
      appendLine("tunnel:")
      appendLine("  mtu: ${SettingsManager.getVpnMtu()}")
      appendLine("  ipv4: ${vpnConfig.ipv4Client}")

      if (KeyValueStorage.decodeSettingsBool(AppConfig.PREF_PREFER_IPV6)) {
        appendLine("  ipv6: '${vpnConfig.ipv6Client}'")
      }

      appendLine("socks5:")
      appendLine("  port: $socksPort")
      appendLine("  address: ${AppConfig.LOOPBACK}")
      appendLine("  udp: 'udp'")

      // Read-write timeout settings
      val timeoutSetting = KeyValueStorage.decodeSettingsString(AppConfig.PREF_HEV_TUNNEL_RW_TIMEOUT) ?: AppConfig.HEVTUN_RW_TIMEOUT
      val parts =
        timeoutSetting.split(",")
          .map { it.trim() }
          .filter { it.isNotEmpty() }
      val tcpTimeout = parts.getOrNull(0)?.toIntOrNull() ?: 300
      val udpTimeout = parts.getOrNull(1)?.toIntOrNull() ?: 60

      appendLine("misc:")
      appendLine("  tcp-read-write-timeout: ${tcpTimeout * 1000}")
      appendLine("  udp-read-write-timeout: ${udpTimeout * 1000}")
      appendLine("  log-level: ${KeyValueStorage.decodeSettingsString(AppConfig.PREF_HEV_TUNNEL_LOGLEVEL) ?: "warn"}")
    }
  }

  /**
   * Stops the tun2socks process
   */
  override fun stopTun2Socks() {
    try {
      Log.i({ "TProxyStopService..." }, AppConfig.TAG)
      TProxyStopService()
    } catch (e: Exception) {
      Log.e({ "Failed to stop hev-socks5-tunnel" }, AppConfig.TAG, e)
    }
  }
}
