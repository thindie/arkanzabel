package com.v2ray.ang.util

import android.util.Log
import com.v2ray.ang.AppConfig
import com.v2ray.ang.AppConfig.LOOPBACK
import com.v2ray.ang.error.ConfigValidationError
import com.v2ray.ang.util.Utils.encode
import com.v2ray.ang.util.Utils.urlDecode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.IDN
import java.net.Inet6Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MalformedURLException
import java.net.Proxy
import java.net.URI
import java.net.URISyntaxException
import java.net.URL

private const val URL_FETCH_MAX_REDIRECTS = 3

/** Per [getUrlContentWithUserAgent] call — not on [HttpUtil] singleton (concurrent fetches would clash). */
private class UrlFetchSession(val maxRedirects: Int = URL_FETCH_MAX_REDIRECTS) {
  val activeConnection = MutableStateFlow<HttpURLConnection?>(null)
  val redirects = MutableStateFlow<Int>(0)
}

object HttpUtil {
  fun toIdnUrl(str: String): String {
    val url = URL(str)
    val host = url.host
    val asciiHost = IDN.toASCII(url.host, IDN.ALLOW_UNASSIGNED)
    return if (host != asciiHost) str.replace(host, asciiHost) else str
  }

  fun toIdnDomain(domain: String): String {
    if (Utils.isPureIpAddress(domain)) return domain
    if (domain.all { it.code < 128 }) return domain
    return IDN.toASCII(domain, IDN.ALLOW_UNASSIGNED)
  }

  fun resolveHostToIP(
    host: String,
    ipv6Preferred: Boolean = false,
  ): List<String>? {
    return try {
      if (Utils.isPureIpAddress(host)) return null
      val addresses = InetAddress.getAllByName(host)
      if (addresses.isEmpty()) return null
      val sortedAddresses =
        if (ipv6Preferred) {
          addresses.sortedWith(compareByDescending { it is Inet6Address })
        } else {
          addresses.sortedWith(compareBy { it is Inet6Address })
        }
      val ipList = sortedAddresses.mapNotNull { it.hostAddress }
      Log.i(AppConfig.TAG, "Resolved IPs for $host: ${ipList.joinToString()}")
      ipList
    } catch (e: IOException) {
      Log.e(AppConfig.TAG, "Failed to resolve host to IP", e)
      null
    }
  }

  fun getUrlContent(
    url: String,
    timeout: Int,
    httpPort: Int = 0,
  ): String? {
    val conn = createProxyConnection(url, httpPort, timeout, timeout) ?: return null
    return try {
      conn.inputStream.bufferedReader().readText()
    } catch (e: IOException) {
      null
    } finally {
      conn.disconnect()
    }
  }

  /**
   * Fetches URL body; follows redirects. Runs on [Dispatchers.IO].
   * State lives in a per-call [UrlFetchSession] (flow + [@Volatile] redirect counter).
   */
  suspend fun getUrlContentWithUserAgent(
    url: String?,
    userAgent: String?,
    timeout: Int = 15000,
    httpPort: Int = 0,
  ): String =
    withContext(Dispatchers.IO) {
      val session = UrlFetchSession()
      coroutineContext.job.invokeOnCompletion { cause ->
        if (cause != null) {
          session.activeConnection.update {
            it?.disconnect()
            null
          }
        }
      }

      var currentUrl = url

      while (session.redirects.value++ < session.maxRedirects) {
        ensureActive()
        if (currentUrl == null) continue
        val conn = createProxyConnection(currentUrl, httpPort, timeout, timeout) ?: continue
        session.activeConnection.value = conn
        try {
          val finalUserAgent =
            if (userAgent.isNullOrBlank()) {
              AppConfig.httpUserAgent
            } else {
              userAgent
            }
          conn.setRequestProperty("User-agent", finalUserAgent)
          conn.connect()

          val responseCode = conn.responseCode
          when (responseCode) {
            in 300..399 -> {
              val location = resolveLocation(conn)
              if (location.isNullOrEmpty()) {
                throw IOException("Redirect location not found")
              }
              currentUrl = location
            }

            else -> return@withContext conn.inputStream.use { it.bufferedReader().readText() }
          }
        } finally {
          session.activeConnection.update { current -> if (current === conn) null else current }
          conn.disconnect()
        }
      }
      throw IOException("Too many redirects")
    }

  fun createProxyConnection(
    urlStr: String,
    port: Int,
    connectTimeout: Int = 15000,
    readTimeout: Int = 15000,
    needStream: Boolean = false,
  ): HttpURLConnection? {
    var conn: HttpURLConnection? = null
    return try {
      val url = URL(urlStr)
      conn =
        if (port == 0) {
          url.openConnection()
        } else {
          url.openConnection(
            Proxy(
              Proxy.Type.HTTP,
              InetSocketAddress(LOOPBACK, port),
            ),
          )
        } as HttpURLConnection

      conn.connectTimeout = connectTimeout
      conn.readTimeout = readTimeout
      if (!needStream) {
        conn.setRequestProperty("Connection", "close")
        conn.instanceFollowRedirects = false
        conn.useCaches = false
      }

      url.userInfo?.let { info ->
        conn.setRequestProperty(
          "Authorization",
          "Basic ${encode(urlDecode(info))}",
        )
      }
      conn
    } catch (e: MalformedURLException) {
      throw ConfigValidationError(
        message = e.message.toString(),
        userReadable = "Url is broken :'(",
      )
    } catch (e: IOException) {
      Log.e(AppConfig.TAG, "Failed to create proxy connection", e)
      conn?.disconnect()
      null
    }
  }

  fun resolveLocation(conn: HttpURLConnection): String? {
    val raw = conn.getHeaderField("Location")?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return resolveRedirectUrl(conn.url, raw)
  }

  private fun resolveRedirectUrl(
    baseUrl: URL,
    raw: String,
  ): String? {
    return try {
      val locUri = URI(raw)
      val baseUri = baseUrl.toURI()
      val resolved = if (locUri.isAbsolute) locUri else baseUri.resolve(locUri)
      resolved.toURL().toString()
    } catch (e: URISyntaxException) {
      resolveRedirectUrlFallback(baseUrl, raw)
    } catch (e: MalformedURLException) {
      resolveRedirectUrlFallback(baseUrl, raw)
    } catch (e: IllegalArgumentException) {
      resolveRedirectUrlFallback(baseUrl, raw)
    }
  }

  private fun resolveRedirectUrlFallback(
    baseUrl: URL,
    raw: String,
  ): String? =
    try {
      URL(raw).toString()
    } catch (e: MalformedURLException) {
      try {
        URL(baseUrl, raw).toString()
      } catch (e2: MalformedURLException) {
        null
      }
    }
}
