package com.v2ray.ang.runtimebuilder

import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.V2rayConfig
import com.v2ray.ang.dto.V2rayConfig.Outbound
import com.v2ray.ang.dto.V2rayConfig.Outbound.StreamSettings
import com.v2ray.ang.enums.Protocol
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OutboundConfigStepTest {
  private fun createSettings(
    muxEnabled: Boolean = false,
    muxConcurrency: String = "8",
    xudpConcurrency: String = "16",
    xudpProxyUDP443: String = "reject",
    preferIpv6: Boolean = false,
    fragmentEnabled: Boolean = false,
    fragmentPackets: String? = null,
  ) = mockk<SettingsReader>(relaxed = true).also {
    every { it.getBool(AppConfig.PREF_MUX_ENABLED, any()) } returns muxEnabled
    every { it.getString(AppConfig.PREF_MUX_CONCURRENCY, any()) } returns muxConcurrency
    every { it.getString(AppConfig.PREF_MUX_XUDP_CONCURRENCY, any()) } returns xudpConcurrency
    every { it.getString(AppConfig.PREF_MUX_XUDP_QUIC, any()) } returns xudpProxyUDP443
    every { it.getBool(AppConfig.PREF_PREFER_IPV6, any()) } returns preferIpv6
    every { it.getBool(AppConfig.PREF_FRAGMENT_ENABLED, any()) } returns fragmentEnabled
    every { it.getString(AppConfig.PREF_FRAGMENT_PACKETS) } returns fragmentPackets
    every { it.getString(AppConfig.PREF_FRAGMENT_LENGTH) } returns null
    every { it.getString(AppConfig.PREF_FRAGMENT_INTERVAL) } returns null
  }

  private fun step(settings: SettingsReader) = OutboundConfigStep(settings, convertProfile2Outbound = { null })

  @Test
  fun `applyGlobalOutboundSettings - mux enabled sets concurrency for a plain protocol`() {
    val outbound = Outbound(protocol = Protocol.Vless.name.lowercase())
    val ok = step(createSettings(muxEnabled = true, muxConcurrency = "4")).applyGlobalOutboundSettings(outbound)

    assertTrue(ok)
    assertTrue(outbound.mux?.enabled == true)
    assertEquals(4, outbound.mux?.concurrency)
  }

  @Test
  fun `applyGlobalOutboundSettings - mux is forced off for protocols that do not support it`() {
    val outbound = Outbound(protocol = Protocol.Trojan.name.lowercase())
    step(createSettings(muxEnabled = true)).applyGlobalOutboundSettings(outbound)

    assertFalse(outbound.mux?.enabled == true)
    assertEquals(-1, outbound.mux?.concurrency)
  }

  @Test
  fun `applyGlobalOutboundSettings - mux is forced off for xhttp network regardless of protocol`() {
    val outbound =
      Outbound(
        protocol = Protocol.Vless.name.lowercase(),
        streamSettings = StreamSettings(network = "xhttp"),
      )
    step(createSettings(muxEnabled = true)).applyGlobalOutboundSettings(outbound)

    assertFalse(outbound.mux?.enabled == true)
  }

  @Test
  fun `applyGlobalOutboundSettings - always sets tcp keepalive idle`() {
    val outbound = Outbound(protocol = Protocol.Vless.name.lowercase())
    step(createSettings()).applyGlobalOutboundSettings(outbound)

    assertEquals(AppConfig.OUTBOUND_TCP_KEEPALIVE_IDLE_SECONDS, outbound.streamSettings?.sockopt?.tcpKeepAliveIdle)
  }

  @Test
  fun `applyGlobalOutboundSettings - wireguard address truncated to one entry when ipv6 not preferred`() {
    val outbound =
      Outbound(
        protocol = Protocol.WireGuard.name.lowercase(),
        settings = Outbound.OutSettings(address = listOf("10.0.0.2/32", "fd00::2/128")),
      )
    step(createSettings(preferIpv6 = false)).applyGlobalOutboundSettings(outbound)

    assertEquals(listOf("10.0.0.2/32"), outbound.settings?.address)
  }

  @Test
  fun `applyOutboundFragment - disabled leaves config untouched`() {
    val config = configWithOutbound(security = AppConfig.TLS)
    val result = step(createSettings(fragmentEnabled = false)).applyOutboundFragment(config)

    assertEquals(1, result.outbounds.size)
  }

  @Test
  fun `applyOutboundFragment - non-TLS non-Reality security leaves config untouched`() {
    val config = configWithOutbound(security = null)
    val result = step(createSettings(fragmentEnabled = true)).applyOutboundFragment(config)

    assertEquals(1, result.outbounds.size)
  }

  @Test
  fun `applyOutboundFragment - reality forces tlshello default to 1-3`() {
    val config = configWithOutbound(security = AppConfig.REALITY)
    val result =
      step(createSettings(fragmentEnabled = true, fragmentPackets = null)).applyOutboundFragment(config)

    val fragment = result.outbounds.first { it.tag == AppConfig.TAG_FRAGMENT }
    assertEquals("1-3", fragment.settings?.fragment?.packets)
  }

  @Test
  fun `applyOutboundFragment - tls forces any non-tlshello packets back to tlshello`() {
    val config = configWithOutbound(security = AppConfig.TLS)
    val result =
      step(createSettings(fragmentEnabled = true, fragmentPackets = "1-3")).applyOutboundFragment(config)

    val fragment = result.outbounds.first { it.tag == AppConfig.TAG_FRAGMENT }
    assertEquals("tlshello", fragment.settings?.fragment?.packets)
  }

  private fun configWithOutbound(security: String?) =
    V2rayConfig(
      log = V2rayConfig.Log(),
      inbounds = arrayListOf(),
      outbounds =
        arrayListOf(
          Outbound(
            protocol = Protocol.Vless.name.lowercase(),
            streamSettings = StreamSettings(security = security),
          ),
        ),
      routing = V2rayConfig.Routing(domainStrategy = "AsIs", rules = arrayListOf()),
    )
}
