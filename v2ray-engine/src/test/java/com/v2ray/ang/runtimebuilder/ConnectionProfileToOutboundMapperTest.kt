package com.v2ray.ang.runtimebuilder

import com.v2ray.ang.dto.ConnectionProfile
import com.v2ray.ang.dto.V2rayConfig.Outbound
import com.v2ray.ang.enums.Protocol
import com.v2ray.ang.protocolstringsparsers.Hysteria2
import com.v2ray.ang.protocolstringsparsers.Shadowsocks
import com.v2ray.ang.protocolstringsparsers.Socks
import com.v2ray.ang.protocolstringsparsers.Trojan
import com.v2ray.ang.protocolstringsparsers.Vless
import com.v2ray.ang.protocolstringsparsers.Vmess
import com.v2ray.ang.protocolstringsparsers.Wireguard
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Pins the protocol -> parser dispatch table; each parser's own logic is tested elsewhere. */
class ConnectionProfileToOutboundMapperTest {
  @AfterTest
  fun tearDown() = unmockkAll()

  private fun profile(protocol: Protocol) = ConnectionProfile(protocol = protocol, subscriptionId = "test")

  @Test
  fun `dispatches each protocol to its own parser`() {
    val marker = Outbound(protocol = "marker")
    mockkObject(Vless, Vmess, Trojan, Hysteria2, Shadowsocks, Socks, Wireguard)
    every { Vless.toOutbound(any()) } returns marker
    every { Vmess.toOutbound(any()) } returns marker
    every { Trojan.toOutbound(any()) } returns marker
    every { Hysteria2.toOutbound(any()) } returns marker
    every { Shadowsocks.toOutbound(any()) } returns marker
    every { Socks.toOutbound(any()) } returns marker
    every { Wireguard.toOutbound(any()) } returns marker

    assertEquals(marker, ConnectionProfileToOutboundMapper.map(profile(Protocol.Vless)))
    assertEquals(marker, ConnectionProfileToOutboundMapper.map(profile(Protocol.Vmess)))
    assertEquals(marker, ConnectionProfileToOutboundMapper.map(profile(Protocol.Trojan)))
    assertEquals(marker, ConnectionProfileToOutboundMapper.map(profile(Protocol.Hysteria2)))
    assertEquals(marker, ConnectionProfileToOutboundMapper.map(profile(Protocol.ShadowSocks)))
    assertEquals(marker, ConnectionProfileToOutboundMapper.map(profile(Protocol.Socks)))
    assertEquals(marker, ConnectionProfileToOutboundMapper.map(profile(Protocol.WireGuard)))
  }

  @Test
  fun `Custom and PolicyGroup are not mapped to an outbound here`() {
    // Both require a stored profile / group resolution handled elsewhere in
    // V2rayConfigManager - this mapper deliberately returns null for them.
    assertNull(ConnectionProfileToOutboundMapper.map(profile(Protocol.Custom)))
    assertNull(ConnectionProfileToOutboundMapper.map(profile(Protocol.PolicyGroup)))
  }
}
