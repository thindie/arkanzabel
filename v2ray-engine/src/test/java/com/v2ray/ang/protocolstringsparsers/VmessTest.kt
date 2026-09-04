package com.v2ray.ang.protocolstringsparsers

import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.VmessQRCode
import com.v2ray.ang.enums.NetworkType
import com.v2ray.ang.enums.Protocol
import com.v2ray.ang.enums.Security
import com.v2ray.ang.runtime.KeyValueStorage
import com.v2ray.ang.testutil.mockAndroidFrameworkStatics
import com.v2ray.ang.util.JsonUtil
import com.v2ray.ang.util.Utils
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class VmessTest {
  @BeforeTest
  fun setUp() {
    mockAndroidFrameworkStatics()
    mockkObject(KeyValueStorage)
    every { KeyValueStorage.decodeSettingsBool(AppConfig.PREF_ALLOW_INSECURE, any()) } returns false
  }

  @AfterTest
  fun tearDown() = unmockkAll()

  @Test
  fun `parse - std query-string form`() {
    val uri =
      AppConfig.VMESS + "11111111-2222-3333-4444-555555555555@1.2.3.4:443" +
        "?type=ws&security=tls&host=example.com&path=%2Fws&sni=example.com&fp=chrome#Std%20Server"

    val profile = requireNotNull(Vmess.parse(uri))

    assertEquals(Protocol.Vmess, profile.protocol)
    assertEquals("1.2.3.4", profile.server)
    assertEquals("443", profile.serverPort)
    assertEquals("11111111-2222-3333-4444-555555555555", profile.password)
    assertEquals(NetworkType.WS, profile.network)
    assertEquals(Security.TLS, profile.security)
    assertEquals("example.com", profile.host)
    assertEquals("/ws", profile.path)
  }

  @Test
  fun `parse - legacy base64-json form`() {
    val json =
      JsonUtil.toJson(
        VmessQRCode().apply {
          v = "2"
          ps = "Legacy Server"
          add = "1.2.3.4"
          port = "443"
          id = "11111111-2222-3333-4444-555555555555"
          aid = "0"
          scy = "auto"
          net = "ws"
          type = "none"
          host = "example.com"
          path = "/ws"
          tls = "tls"
          sni = "example.com"
          fp = "chrome"
          insecure = "0"
        },
      )
    val uri = AppConfig.VMESS + Utils.encode(json)

    val profile = requireNotNull(Vmess.parse(uri))

    assertEquals(Protocol.Vmess, profile.protocol)
    assertEquals("Legacy Server", profile.remarks)
    assertEquals("1.2.3.4", profile.server)
    assertEquals("443", profile.serverPort)
    assertEquals(NetworkType.WS, profile.network)
    assertEquals(Security.TLS, profile.security)
    assertEquals("example.com", profile.host)
    assertEquals(false, profile.insecure)
  }

  @Test
  fun `parse - legacy form with blank tls field yields null security`() {
    // Before ConnectionProfile.security became a typed enum, the legacy path assigned
    // `security = vmessQRCode.tls` directly - VmessQRCode.tls defaults to "" (non-nullable),
    // so an omitted tls field used to produce `security == ""`, not null as in the other
    // protocol parsers. Security.fromString("") now normalizes that to null, matching
    // every other parser's "absent security" representation.
    val json =
      JsonUtil.toJson(
        VmessQRCode().apply {
          add = "1.2.3.4"
          port = "443"
          id = "uuid"
          net = "tcp"
        },
      )
    val uri = AppConfig.VMESS + Utils.encode(json)

    val profile = requireNotNull(Vmess.parse(uri))

    assertEquals(null, profile.security)
  }

  @Test
  fun `parse - dispatch prefers std form when both are query-like`() {
    // Vmess.parse's own heuristic (indexOf('?') and indexOf('&') both present) routes to
    // parseVmessStd first. ProfileUriParser additionally tries parseVmessStd() before
    // falling back to parse() - two different, not-fully-aligned dispatch rules for the
    // same protocol (see ProfileUriParser.kt). This test pins Vmess.parse's own rule only.
    val uri = AppConfig.VMESS + "uuid@1.2.3.4:443?type=tcp&security=none#x"

    val profile = requireNotNull(Vmess.parse(uri))

    assertEquals("1.2.3.4", profile.server)
    assertEquals(NetworkType.TCP, profile.network)
  }
}
