package com.v2ray.ang.runtime

import android.content.Context
import android.os.SystemClock
import com.google.errorprone.annotations.Immutable
import com.thindie.engine.core.Log
import com.thindie.rknzbl.v2rayengine.R
import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.IPAPIInfo
import com.v2ray.ang.util.HttpUtil
import com.v2ray.ang.util.JsonUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.UnknownHostException

object SpeedtestManager {
  private val tcpTestingSockets = MutableStateFlow<List<Socket>>(emptyList())

  /**
   * Measures the TCP connection time to a given URL and port.
   *
   * @param url The URL to connect to.
   * @param port The port to connect to.
   * @return The connection time in milliseconds, or -1 if the connection failed.
   */
  suspend fun tcping(
    url: String,
    port: Int,
  ): Long {
    return buildList {
      repeat(2) {
        add(socketConnectTime(url, port))
      }
    }
      .filter { it > 0 }
      .minOrNull() ?: -1L
  }

  /**
   * Measures the time taken to establish a TCP connection to a given URL and port.
   *
   * @param url The URL to connect to.
   * @param port The port to connect to.
   * @return The connection time in milliseconds, or -1 if the connection failed.
   */
  suspend fun socketConnectTime(
    url: String,
    port: Int,
  ): Long =
    withContext(Dispatchers.IO) {
      val socket = Socket()
      tcpTestingSockets.update { sockets -> sockets + socket }
      try {
        val start = System.currentTimeMillis()
        socket.connect(InetSocketAddress(url, port), 3000)
        System.currentTimeMillis() - start
      } catch (e: UnknownHostException) {
        Log.e({ "Unknown host: $url" }, AppConfig.TAG, e)
        -1L
      } catch (e: IOException) {
        Log.e({ "socketConnectTime IOException" }, AppConfig.TAG, e)
        -1L
      } finally {
        tcpTestingSockets.update { sockets -> sockets - socket }
        try {
          socket.close()
        } catch (e: IOException) {
          Log.e({ "Failed to close test socket" }, AppConfig.TAG, e)
        }
      }
    }

  /**
   * Closes all TCP sockets that are currently being tested.
   */
  fun closeAllTcpSockets() {
    val sockets = tcpTestingSockets.value
    sockets.forEach { socket ->
      try {
        socket.close()
      } catch (e: IOException) {
        Log.e({ "Failed to close test socket" }, AppConfig.TAG, e)
      }
    }
    tcpTestingSockets.value = emptyList()
  }

  /**
   * Tests the connection to a given URL and port.
   *
   * @param context The Context in which the test is running.
   * @param port The port to connect to.
   * @return A pair containing the elapsed time in milliseconds and the result message.
   */
  suspend fun testConnection(
    context: Context,
    port: Int,
  ): SpeedTestResult? =
    withContext(Dispatchers.IO) {
      var result: String
      var error: Throwable? = null
      var elapsed: Long

      val conn =
        HttpUtil.createProxyConnection(
          urlStr = SettingsManager.getDelayTestUrl(),
          port = port,
          connectTimeout = 15000,
          readTimeout = 15000,
        )
          ?: return@withContext null
      try {
        val start = SystemClock.elapsedRealtime()
        val code = conn.responseCode
        elapsed = SystemClock.elapsedRealtime() - start

        result =
          when {
            code == 204 -> context.getString(R.string.connection_test_available, elapsed)
            code == 200 && conn.contentLengthLong == 0L ->
              context.getString(
                R.string.connection_test_available,
                elapsed,
              )
            code in 400..550 -> {
              throw IOException(
                context.getString(R.string.connection_test_error_status_code, code),
              )
            }
            else -> return@withContext null
          }
      } catch (e: IOException) {
        Log.e({ "Connection test IOException" }, AppConfig.TAG, e)
        error =
          IOException(
            context.getString(R.string.connection_test_error, e.message),
          )
        result = ""
      } finally {
        conn.disconnect()
        closeAllTcpSockets()
      }
      if (error != null) {
        SpeedTestResult.Err(error.message!!)
      } else {
        SpeedTestResult.Ok(result)
      }
    }

  @Immutable
  sealed interface SpeedTestResult {
    @JvmInline
    value class Ok(val value: String) : SpeedTestResult

    @JvmInline
    value class Err(val value: String) : SpeedTestResult

    val message get() =
      when (this) {
        is Err -> value
        is Ok -> value
      }
  }

  /**
   * Measures download throughput through a proxy connection by downloading
   * data and timing it. Returns bytes per second, or -1 on failure.
   */
  suspend fun measureBandwidth(
    context: Context,
    port: Int,
    testUrl: String = SettingsManager.getDelayTestUrl(),
  ): Long {
    return withContext(Dispatchers.IO) {
      val conn =
        HttpUtil.createProxyConnection(testUrl, port, 15000, 30000)
          ?: return@withContext -1L
      try {
        if (conn.responseCode != 200 && conn.responseCode != 204) return@withContext -1L

        val start = SystemClock.elapsedRealtime()
        var totalBytes = 0L
        conn.inputStream.use { input ->
          val buffer = ByteArray(8192)
          while (true) {
            val read = input.read(buffer)
            if (read == -1) break
            totalBytes += read
            // Cap at ~5MB to keep test bounded
            if (totalBytes >= 5 * 1024 * 1024) break
          }
        }
        val elapsedMs = SystemClock.elapsedRealtime() - start
        if (elapsedMs == 0L || totalBytes == 0L) return@withContext -1L

        // Return bytes per second
        (totalBytes * 1000L / elapsedMs).toInt().toLong()
      } catch (e: IOException) {
        Log.e({ "Bandwidth test failed" }, AppConfig.TAG, e)
        -1L
      } finally {
        conn.disconnect()
      }
    }
  }

  suspend fun getRemoteIPInfo(): String? =
    withContext(Dispatchers.IO) {
      val url =
        KeyValueStorage.decodeSettingsString(AppConfig.PREF_IP_API_URL)
          ?.ifBlank { null } ?: AppConfig.IP_API_URL

      val httpPort = SettingsManager.getHttpPort()
      val content = HttpUtil.getUrlContent(url, 5000, httpPort) ?: return@withContext null
      val ipInfo = JsonUtil.fromJson(content, IPAPIInfo::class.java) ?: return@withContext null

      val ip =
        listOf(
          ipInfo.ip,
          ipInfo.clientIp,
          ipInfo.ipAddr,
          ipInfo.query,
        ).firstOrNull { !it.isNullOrBlank() }

      val country =
        listOf(
          ipInfo.countryCodeSnake,
          ipInfo.country,
          ipInfo.countryCode,
          ipInfo.location?.countryCode,
        ).firstOrNull { !it.isNullOrBlank() }

      "(${country ?: "unknown"}) ${ip ?: "unknown"}"
    }
}
