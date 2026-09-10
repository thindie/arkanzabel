package com.v2ray.ang.protocolstringsparsers

import com.v2ray.ang.enums.Protocol
import com.v2ray.ang.testutil.mockAndroidFrameworkStatics
import com.v2ray.ang.util.Utils
import io.mockk.unmockkAll
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ShadowsocksTest {
  @BeforeTest
  fun setUp() = mockAndroidFrameworkStatics()

  @AfterTest
  fun tearDown() = unmockkAll()

  @Test
  fun `parse - SIP002 form with plain userinfo`() {
    val profile = requireNotNull(Shadowsocks.parse("ss://aes-256-gcm:password123@1.2.3.4:8388#My%20Server"))

    assertEquals(Protocol.ShadowSocks, profile.protocol)
    assertEquals("1.2.3.4", profile.server)
    assertEquals("8388", profile.serverPort)
    assertEquals("aes-256-gcm", profile.method)
    assertEquals("password123", profile.password)
    assertEquals("My Server", profile.remarks)
  }

  @Test
  fun `parse - SIP002 form with base64 userinfo`() {
    val userInfo = Utils.encode("aes-256-gcm:password123", removePadding = true)

    val profile = requireNotNull(Shadowsocks.parse("ss://$userInfo@1.2.3.4:8388#Encoded%20Server"))

    assertEquals("aes-256-gcm", profile.method)
    assertEquals("password123", profile.password)
  }

  @Test
  fun `parse - SIP002 obfs-http plugin sets host and path`() {
    val plugin = Utils.encodeURIComponent("obfs-local;obfs=http;obfs-host=example.com;path=/ws")
    val profile =
      requireNotNull(
        Shadowsocks.parse("ss://aes-256-gcm:password123@1.2.3.4:8388?plugin=$plugin#x"),
      )

    assertEquals("http", profile.headerType)
    assertEquals("example.com", profile.host)
    assertEquals("/ws", profile.path)
  }

  @Test
  fun `parse - legacy fully-encoded form`() {
    // Whole "method:password@host:port" is base64-encoded with no visible '@' before
    // decoding, so parseSip002's URI parse sees no userInfo and returns null, falling
    // through to parseLegacy's regex-based decode.
    val encoded = Utils.encode("aes-256-gcm:password123@1.2.3.4:8388")

    val profile = requireNotNull(Shadowsocks.parse("ss://$encoded#Legacy%20Server"))

    assertEquals(Protocol.ShadowSocks, profile.protocol)
    assertEquals("1.2.3.4", profile.server)
    assertEquals("8388", profile.serverPort)
    assertEquals("aes-256-gcm", profile.method)
    assertEquals("password123", profile.password)
    assertEquals("Legacy Server", profile.remarks)
  }
}
