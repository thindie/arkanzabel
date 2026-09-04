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

class Hysteria2Test {
  @BeforeTest
  fun setUp() {
    mockAndroidFrameworkStatics()
    mockkObject(KeyValueStorage)
    every { KeyValueStorage.decodeSettingsBool(AppConfig.PREF_ALLOW_INSECURE, any()) } returns false
  }

  @AfterTest
  fun tearDown() = unmockkAll()

  @Test
  fun `parse - no query string defaults to TLS over hysteria network`() {
    val profile = requireNotNull(Hysteria2.parse("hysteria2://password@example.com:443#My%20Server"))

    assertEquals(Protocol.Hysteria2, profile.protocol)
    assertEquals("example.com", profile.server)
    assertEquals("443", profile.serverPort)
    assertEquals("password", profile.password)
    assertEquals(Security.TLS, profile.security)
    assertEquals(NetworkType.HYSTERIA, profile.network)
  }

  @Test
  fun `parse - obfuscation and port hopping fields`() {
    val profile =
      requireNotNull(
        Hysteria2.parse(
          "hysteria2://password@example.com:443" +
            "?obfs=salamander&obfs-password=secret&mport=20000-30000&mportHopInt=30&pinSHA256=AABBCC",
        ),
      )

    assertEquals("secret", profile.obfsPassword)
    assertEquals("20000-30000", profile.portHopping)
    assertEquals("30", profile.portHoppingInterval)
    assertEquals("AABBCC", profile.pinnedCA256)
  }

  @Test
  fun `hy2 scheme alias is accepted`() {
    // AppConfig.HY2 ("hy2://") is a recognized alias dispatched to the same parser by
    // ProfileUriParser - verified directly here since Hysteria2.parse itself is scheme-agnostic
    // (it only looks at the authority/query, not the scheme name).
    val profile = requireNotNull(Hysteria2.parse("hy2://password@example.com:443#x"))

    assertEquals("example.com", profile.server)
  }
}
