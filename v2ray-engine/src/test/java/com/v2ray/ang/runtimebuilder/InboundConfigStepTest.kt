package com.v2ray.ang.runtimebuilder

import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.V2rayConfig
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InboundConfigStepTest {
  private fun socksInbound() =
    V2rayConfig.Inbound(
      tag = "socks",
      port = 0,
      protocol = "socks",
      listen = "0.0.0.0",
      sniffing =
        V2rayConfig.Inbound.Sniffing(
          enabled = false,
          destOverride = arrayListOf("http", "tls"),
        ),
    )

  private fun configWith(vararg inbounds: V2rayConfig.Inbound) =
    V2rayConfig(
      log = V2rayConfig.Log(),
      inbounds = arrayListOf(*inbounds),
      outbounds = arrayListOf(),
      routing = V2rayConfig.Routing(domainStrategy = "AsIs", rules = arrayListOf()),
    )

  private fun createSettings(
    socksPort: Int = 10808,
    proxySharing: Boolean = false,
    fakeDns: Boolean = false,
    sniffingEnabled: Boolean = true,
    routeOnly: Boolean = false,
    vpnMtu: Int = 1500,
  ) = mockk<SettingsReader>(relaxed = true).also {
    every { it.getInt(AppConfig.PREF_SOCKS_PORT, any()) } returns socksPort
    every { it.getBool(AppConfig.PREF_PROXY_SHARING, any()) } returns proxySharing
    every { it.getBool(AppConfig.PREF_FAKE_DNS_ENABLED, any()) } returns fakeDns
    every { it.getBool(AppConfig.PREF_SNIFFING_ENABLED, any()) } returns sniffingEnabled
    every { it.getBool(AppConfig.PREF_ROUTE_ONLY_ENABLED, any()) } returns routeOnly
    every { it.getInt(AppConfig.PREF_VPN_MTU, any()) } returns vpnMtu
  }

  @Test
  fun `applyInbounds - proxy sharing disabled forces loopback listen`() {
    val step = InboundConfigStep(createSettings(proxySharing = false), needTun = { false })

    val result = step.applyInbounds(configWith(socksInbound()))

    assertEquals(AppConfig.LOOPBACK, result.inbounds[0].listen)
  }

  @Test
  fun `applyInbounds - proxy sharing enabled leaves listen untouched`() {
    val step = InboundConfigStep(createSettings(proxySharing = true), needTun = { false })

    val result = step.applyInbounds(configWith(socksInbound()))

    assertEquals("0.0.0.0", result.inbounds[0].listen)
  }

  @Test
  fun `applyInbounds - sets socks port from settings`() {
    val step = InboundConfigStep(createSettings(socksPort = 12345), needTun = { false })

    val result = step.applyInbounds(configWith(socksInbound()))

    assertEquals(12345, result.inbounds[0].port)
  }

  @Test
  fun `applyInbounds - fakedns enabled adds destOverride entry and enables sniffing`() {
    val step = InboundConfigStep(createSettings(fakeDns = true, sniffingEnabled = false), needTun = { false })

    val result = step.applyInbounds(configWith(socksInbound()))

    assertTrue(result.inbounds[0].sniffing!!.enabled)
    assertTrue("fakedns" in result.inbounds[0].sniffing!!.destOverride)
  }

  @Test
  fun `applyInbounds - sniffing disabled and fakedns off clears destOverride`() {
    val step = InboundConfigStep(createSettings(fakeDns = false, sniffingEnabled = false), needTun = { false })

    val result = step.applyInbounds(configWith(socksInbound()))

    assertFalse(result.inbounds[0].sniffing!!.enabled)
    assertTrue(result.inbounds[0].sniffing!!.destOverride.isEmpty())
  }

  @Test
  fun `applyInbounds - adds an http mirror inbound alongside socks`() {
    val step = InboundConfigStep(createSettings(socksPort = 10808), needTun = { false })

    val result = step.applyInbounds(configWith(socksInbound()))

    assertEquals(2, result.inbounds.size)
    val httpInbound = result.inbounds[1]
    assertEquals("http", httpInbound.tag)
    assertEquals("http", httpInbound.protocol)
    // hostApplicationId defaults to "com.thindie.rknzbl" (not "com.v2ray.ang"), so
    // Utils.isXray() is false here and the http mirror listens one port above socks.
    assertEquals(10809, httpInbound.port)
  }

  @Test
  fun `applyInbounds - needTun copies mtu and sniffing onto the tun inbound`() {
    val tunInbound =
      V2rayConfig.Inbound(
        tag = "tun",
        port = 0,
        protocol = "socks",
        settings = V2rayConfig.Inbound.InSettings(),
      )
    val step = InboundConfigStep(createSettings(vpnMtu = 9000), needTun = { true })

    val result = step.applyInbounds(configWith(socksInbound(), tunInbound))

    val tun = result.inbounds.first { it.tag == "tun" }
    assertEquals(9000, tun.settings?.mtu)
    assertEquals(result.inbounds[0].sniffing, tun.sniffing)
  }
}
