package com.v2ray.ang.runtime

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.Gson
import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.ConnectionProfile
import com.v2ray.ang.runtimebuilder.ConnectionProfileToOutboundMapper
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

@RunWith(AndroidJUnit4::class)
class CoreLoopSpeedtestInstrumentedTest {
  private var controller: CoreController? = null

  @After fun tearDown() {
    try {
      controller?.stopLoop()
    } catch (_: Exception) {
    }
    controller = null
  }

  @Test fun scanProfiles() {
    val ctx = InstrumentationRegistry.getInstrumentation().targetContext
    val assetText = ctx.assets.open("profiles.txt").bufferedReader().readText()
    val urls = assetText.split("\n").map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }
    // Scale later; start small to validate the flow end-to-end.
    val probeUrls = urls.take(3)

    val results = mutableListOf<String>()
    var liveCount = 0

    for (url in probeUrls) {
      val profile = ProfileUriParser.parse(url) ?: continue
      val outboundJson = buildOutbound(profile) ?: continue
      val socksPort = findFreeSocksPort()
      val cfgJson = buildConfig(outboundJson, socksPort)
      controller = newCoreController(createCallback())

      var live = false
      var latencyMs = 0L
      var bytesPerSec = 0L
      try {
        val started = java.util.concurrent.atomic.AtomicBoolean(false)
        val startThread =
          Thread {
            controller!!.startLoop(cfgJson, 0)
            started.set(true)
          }
        startThread.start()
        startThread.join(15000)
        if (!started.get()) {
          throw RuntimeException("startLoop timed out")
        }
        assertTrue("core must run for ${profile.server}", controller!!.isRunning)

        val r = probeThroughProxy(socksPort, profile)
        live = r.first
        latencyMs = r.second
        bytesPerSec = r.third
      } catch (e: Exception) {
        results.add("${profile.protocol}:${profile.server}:${profile.serverPort} -> ERROR (${e.message})")
      } finally {
        try {
          controller?.stopLoop()
        } catch (_: Exception) {
        }
      }

      if (live) liveCount++
      val status = if (live) "LIVE" else "DEAD"
      results.add("$status ${profile.protocol}:${profile.server}:${profile.serverPort} (${latencyMs}ms, $bytesPerSec/s)")
    }

    println("SCAN RESULTS:\n" + results.joinToString("\n"))
    println("TOTAL LIVE: $liveCount / ${probeUrls.size}")
    assertTrue("expected at least one live profile", liveCount > 0)
  }

  private fun newCoreController(cb: CoreCallbackHandler): CoreController =
    CoreController::class.java.getDeclaredConstructor(CoreCallbackHandler::class.java).apply { isAccessible = true }.newInstance(cb)

  private fun createCallback(): CoreCallbackHandler =
    object : CoreCallbackHandler {
      override fun startup(): Long = 0

      override fun shutdown(): Long = 0

      override fun onEmitStatus(
        status: Long,
        payload: String,
      ): Long = 0
    }

  // Build outbound JSON via the app's universal protocol mapper (protocol-independent).
  private fun buildOutbound(profile: ConnectionProfile): String? {
    val outbound = ConnectionProfileToOutboundMapper.map(profile) ?: return null
    return Gson().toJson(outbound)
  }

  private fun buildConfig(
    outboundJson: String,
    port: Int,
  ): String {
    val socksInbound =
      """{"tag":"loopback","listen":"127.0.0.1","port":PORT,"protocol":"socks","settings":{"auth":"noauth","udp":true}}"""
        .replace("PORT", port.toString())
    return """{"log":{"access":"","error":"","level":"none"},"dns":{"servers":["8.8.8.8"]},"inbounds":[INB],"outbounds":[OUTBOUND]}"""
      .replace("PORT", port.toString())
      .replace("INB", socksInbound)
      .replace("OUTBOUND", outboundJson)
  }

  private fun findFreeSocksPort(): Int {
    val s = Socket()
    try {
      s.connect(InetSocketAddress(AppConfig.LOOPBACK, 0), 500)
      return s.localPort
    } catch (_: Exception) {
    }
    for (p in 20000..21000) {
      val t = java.net.ServerSocket(p, 0, InetAddress.getByName(AppConfig.LOOPBACK))
      try {
        t.close()
        return p
      } catch (
        _: Exception,
      ) {
        t.close()
      }
    }
    return 25000
  }

  // Probe THROUGH the loopback proxy to the profile's OWN server IP + SNI.
  // Real traffic through the core = success; dead profile -> timeout/error.
  private fun probeThroughProxy(
    socksPort: Int,
    profile: ConnectionProfile,
  ): Triple<Boolean, Long, Long> {
    val start = System.currentTimeMillis()
    val targetHost = profile.server ?: return Triple(false, -1L, 0L)
    val targetPort = profile.serverPort?.toIntOrNull() ?: return Triple(false, -1L, 0L)

    // 1. SOCKS5 CONNECT through loopback proxy to the profile's server.
    val proxySocket = Socket()
    proxySocket.connect(InetSocketAddress(AppConfig.LOOPBACK, socksPort), 8000)
    proxySocket.soTimeout = 8000
    val out: OutputStream = proxySocket.outputStream
    val dataIn = DataInputStream(proxySocket.inputStream)

    out.write(byteArrayOf(0x05, 0x01, 0x00)) // no-auth
    val authResp = ByteArray(2)
    readExact(dataIn, authResp)
    if (authResp[0].toInt() != 0x05 || authResp[1].toInt() != 0x00) return Triple(false, -1L, 0L)

    // Resolve target to IP for SOCKS5 CONNECT.
    val resolvedIp = InetAddress.getByName(targetHost)
    val ipParts = resolvedIp.hostAddress.split(".")

    out.write(
      byteArrayOf(
        0x05,
        0x03,
        0x00,
        0x01,
        ipParts[0].toByte(),
        ipParts[1].toByte(),
        ipParts[2].toByte(),
        ipParts[3].toByte(),
        (targetPort shr 8).toByte(),
        targetPort.toByte(),
      ),
    )
    val resp = ByteArray(4)
    readExact(dataIn, resp)
    if (resp[0].toInt() != 0x05 || resp[1].toInt() != 0x00) return Triple(false, -1L, 0L)
    when (resp[3].toInt()) {
      0x01 -> readExact(dataIn, ByteArray(6))
      0x04 -> readExact(dataIn, ByteArray(18))
      else -> {
        val l = dataIn.read().toInt()
        readExact(dataIn, ByteArray(l))
        readExact(dataIn, ByteArray(2))
      }
    }

    // 2. TLS handshake through proxy with the profile's SNI (real SSLSocket).
    val trustManagers =
      arrayOf<javax.net.ssl.TrustManager>(
        object : X509TrustManager {
          override fun checkClientTrusted(
            chain: Array<out X509Certificate>?,
            authType: String?,
          ) {}

          override fun checkServerTrusted(
            chain: Array<out X509Certificate>?,
            authType: String?,
          ) {}

          override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        },
      )
    val ctx = SSLContext.getInstance("TLS")
    ctx.init(null, trustManagers, java.security.SecureRandom())
    val factory: SSLSocketFactory = ctx.socketFactory
    val tlsSocket = factory.createSocket(proxySocket, targetHost, targetPort, true) as SSLSocket
    tlsSocket.soTimeout = 8000
    // SNI via reflection (Android SSLSocket has no public setter).
    try {
      val setSniMethod = SSLSocket::class.java.getDeclaredMethod("setSniHostnames", List::class.java)
      setSniMethod.isAccessible = true
      setSniMethod.invoke(tlsSocket, profile.sni?.ifEmpty { listOf(targetHost) })
    } catch (_: Exception) {
    }

    // 3. HTTP GET with WS-upgrade header (path from the profile).
    val path = profile.path ?: "/assignment"
    val hostHeader = if (profile.host?.isNotEmpty() == true) profile.host else targetHost
    val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
    val httpReq =
      "GET $path HTTP/1.1\r\n" +
        "Host: $hostHeader\r\n" +
        "Upgrade: websocket\r\n" +
        "Connection: Upgrade\r\n" +
        "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\n" +
        "Sec-WebSocket-Version: 13\r\n" +
        "User-Agent: $userAgent\r\n" +
        "Accept: */*\r\n\r\n"
    tlsSocket.outputStream.write(httpReq.toByteArray())
    tlsSocket.outputStream.flush()

    // Read response header, check for 101 Switching Protocols.
    val buf = ByteArrayOutputStream()
    var totalBytes = 0L
    val deadline = System.currentTimeMillis() + 8000
    while (System.currentTimeMillis() < deadline) {
      if (buf.toString().contains("101")) break
      val n = tlsSocket.inputStream.read()
      if (n == -1) break
      buf.write(n)
      totalBytes++
      if (totalBytes > 2048) break
    }
    val header = buf.toString()
    val isLive = header.contains("101")
    val latencyMs = System.currentTimeMillis() - start
    val elapsedSec = (System.currentTimeMillis() - start) / 1000.0
    val bytesPerSec = if (elapsedSec > 0) (totalBytes / elapsedSec).toLong() else 0L

    try {
      tlsSocket.close()
    } catch (_: Exception) {
    }
    return Triple(isLive, latencyMs, bytesPerSec)
  }

  // Read exactly n bytes with bounded retries to avoid indefinite hangs.
  private fun readExact(
    inStream: DataInputStream,
    buf: ByteArray,
  ) {
    var off = 0
    val deadline = System.currentTimeMillis() + 8000
    while (off < buf.size && System.currentTimeMillis() < deadline) {
      val n = inStream.read(buf, off, buf.size - off)
      if (n <= 0) break
      off += n
    }
  }
}
