package com.v2ray.ang.protocolstringsparsers

import com.v2ray.ang.AppConfig
import com.v2ray.ang.enums.NetworkType
import com.v2ray.ang.enums.Protocol
import com.v2ray.ang.enums.Security
import com.v2ray.ang.runtime.KeyValueStorage
import com.v2ray.ang.testutil.mockAndroidFrameworkStatics
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TrojanTest {
  @BeforeTest
  fun setUp() {
    mockAndroidFrameworkStatics()
    mockkObject(KeyValueStorage)
    every { KeyValueStorage.decodeSettingsBool(AppConfig.PREF_ALLOW_INSECURE, any()) } returns false
  }

  @AfterTest
  fun tearDown() = unmockkAll()

  @Test
  fun `parse - no query string defaults to plain TLS over TCP`() {
    // Unlike Vless (returns null without a query string), Trojan builds a valid profile
    // with TLS defaults. Real behavioral divergence between parsers, pinned as-is.
    val profile = requireNotNull(Trojan.parse("trojan://secret@example.com:443#My%20Server"))

    assertEquals(Protocol.Trojan, profile.protocol)
    assertEquals("example.com", profile.server)
    assertEquals("443", profile.serverPort)
    assertEquals("secret", profile.password)
    assertEquals(NetworkType.TCP, profile.network)
    assertEquals(Security.TLS, profile.security)
    assertEquals("My Server", profile.remarks)
  }

  @Test
  fun `parse - with query string reads transport and security`() {
    val profile =
      requireNotNull(
        Trojan.parse("trojan://secret@example.com:443?security=tls&sni=example.com&type=grpc&serviceName=svc#x"),
      )

    assertEquals(Security.TLS, profile.security)
    assertEquals("example.com", profile.sni)
    assertEquals(NetworkType.GRPC, profile.network)
    assertEquals("svc", profile.serviceName)
  }

  @Test
  fun `parse - unrecognized security value falls back to TLS, not passed through raw`() {
    // Before ConnectionProfile.security became a typed enum, Trojan re-derived `security`
    // directly from the raw query value (`queryParam["security"] ?: AppConfig.TLS`),
    // bypassing getItemFormQuery's tls-or-reality-or-null filter - so a garbage value
    // like "garbage" used to survive as a literal string here, unlike Vless which dropped
    // it to null. Typing the field made that impossible: Security.fromString("garbage")
    // is null, so this now falls back to the same TLS default as a missing value.
    val profile = requireNotNull(Trojan.parse("trojan://secret@example.com:443?security=garbage#x"))

    assertEquals(Security.TLS, profile.security)
  }

  @Test
  fun `parse - insecure flag from query`() {
    val profile = requireNotNull(Trojan.parse("trojan://secret@example.com:443?security=tls&insecure=1#x"))

    assertTrue(profile.insecure)
  }
}
