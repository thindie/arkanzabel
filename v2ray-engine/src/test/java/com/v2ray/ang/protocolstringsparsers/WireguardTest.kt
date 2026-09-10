package com.v2ray.ang.protocolstringsparsers

import com.v2ray.ang.enums.Protocol
import com.v2ray.ang.testutil.mockAndroidFrameworkStatics
import io.mockk.unmockkAll
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WireguardTest {
  @BeforeTest
  fun setUp() = mockAndroidFrameworkStatics()

  @AfterTest
  fun tearDown() = unmockkAll()

  @Test
  fun `parse - uri form`() {
    val profile =
      requireNotNull(
        Wireguard.parse(
          "wireguard://cHJpdmF0ZWtleQ==@1.2.3.4:51820" +
            "?address=10.0.0.2%2F32&publickey=cHVibGlja2V5&presharedkey=cHNr&mtu=1420&reserved=1%2C2%2C3#WG",
        ),
      )

    assertEquals(Protocol.WireGuard, profile.protocol)
    assertEquals("1.2.3.4", profile.server)
    assertEquals("51820", profile.serverPort)
    assertEquals("cHJpdmF0ZWtleQ==", profile.secretKey)
    assertEquals("10.0.0.2/32", profile.localAddress)
    assertEquals("cHVibGlja2V5", profile.publicKey)
    assertEquals("cHNr", profile.preSharedKey)
    assertEquals(1420, profile.mtu)
    assertEquals("1,2,3", profile.reserved)
  }

  @Test
  fun `parse - no query string returns null`() {
    assertNull(Wireguard.parse("wireguard://cHJpdmF0ZWtleQ==@1.2.3.4:51820#WG"))
  }

  @Test
  fun `parseWireguardConfFile - ini-style conf`() {
    val conf =
      """
      [Interface]
      PrivateKey = cHJpdmF0ZWtleQ==
      Address = 10.0.0.2/32
      MTU = 1420

      [Peer]
      PublicKey = cHVibGlja2V5
      PresharedKey = cHNr
      Endpoint = 1.2.3.4:51820
      """.trimIndent()

    val profile = requireNotNull(Wireguard.parseWireguardConfFile(conf))

    assertEquals("cHJpdmF0ZWtleQ==", profile.secretKey)
    assertEquals("10.0.0.2/32", profile.localAddress)
    assertEquals(1420, profile.mtu)
    assertEquals("cHVibGlja2V5", profile.publicKey)
    assertEquals("cHNr", profile.preSharedKey)
    assertEquals("1.2.3.4", profile.server)
    assertEquals("51820", profile.serverPort)
  }
}
