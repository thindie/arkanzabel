package com.v2ray.ang.runtimebuilder

import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.V2rayConfig
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DnsConfigStepTest {
  private fun createMockSettings(
    localDnsEnabled: Boolean = false,
    fakeDnsEnabled: Boolean = false,
    dnsRefreshInterval: Int = 0,
    userHosts: String? = null,
    remoteDns: List<String> = listOf("8.8.8.8"),
    domesticDns: List<String> = listOf("1.1.1.1"),
    vpnMode: Boolean = false,
    usingHevTun: Boolean = false,
  ) = mockk<SettingsReader>(relaxed = true).also {
    every { it.getBool(AppConfig.PREF_LOCAL_DNS_ENABLED, any()) } returns localDnsEnabled
    every { it.getBool(AppConfig.PREF_FAKE_DNS_ENABLED, any()) } returns fakeDnsEnabled
    every { it.getInt(AppConfig.PREF_DNS_REFRESH_INTERVAL, any()) } returns dnsRefreshInterval
    every { it.getString(AppConfig.PREF_DNS_HOSTS) } returns userHosts
    every { it.getRemoteDnsServers() } returns remoteDns
    every { it.getDomesticDnsServers() } returns domesticDns
    every { it.isVpnMode() } returns vpnMode
    every { it.isUsingHevTun() } returns usingHevTun
  }

  private fun createStep(settings: SettingsReader = createMockSettings()) =
    DnsConfigStep(
      settings = settings,
      getUserRule2Domain = { arrayListOf<String>() },
    )

  @Test
  fun `applyFakeDns - does not enable fakedns when local DNS disabled`() {
    val config =
      V2rayConfig(
        log = V2rayConfig.Log(),
        inbounds = arrayListOf(),
        outbounds = arrayListOf(),
        routing = V2rayConfig.Routing(domainStrategy = "", rules = arrayListOf()),
      )

    createStep(settings = createMockSettings(localDnsEnabled = false, fakeDnsEnabled = true))
      .applyFakeDns(config)

    assertNull(config.fakedns, "fakedns should not be set when local DNS is disabled")
  }

  @Test
  fun `applyFakeDns - does not enable fakedns when fake DNS disabled`() {
    val config =
      V2rayConfig(
        log = V2rayConfig.Log(),
        inbounds = arrayListOf(),
        outbounds = arrayListOf(),
        routing = V2rayConfig.Routing(domainStrategy = "", rules = arrayListOf()),
      )

    createStep(settings = createMockSettings(localDnsEnabled = true, fakeDnsEnabled = false))
      .applyFakeDns(config)

    assertNull(config.fakedns, "fakedns should not be set when fake DNS is disabled")
  }

  @Test
  fun `applyFakeDns - enables fakedns when both flags are true`() {
    val config =
      V2rayConfig(
        log = V2rayConfig.Log(),
        inbounds = arrayListOf(),
        outbounds = arrayListOf(),
        routing = V2rayConfig.Routing(domainStrategy = "", rules = arrayListOf()),
      )

    createStep(settings = createMockSettings(localDnsEnabled = true, fakeDnsEnabled = true))
      .applyFakeDns(config)

    assertTrue(config.fakedns != null, "fakedns should be set when both flags are true")
  }

  @Test
  fun `applyDns - sets up DNS servers for proxy and direct`() {
    val config =
      V2rayConfig(
        log = V2rayConfig.Log(),
        inbounds = arrayListOf(),
        outbounds = arrayListOf(),
        routing = V2rayConfig.Routing(domainStrategy = "", rules = arrayListOf()),
      )

    createStep().applyDns(config)

    assertTrue(config.dns != null, "dns should be set")
    assertTrue(config.dns!!.servers?.isNotEmpty() ?: false, "should have at least one DNS server")
  }

  @Test
  fun `applyDns - includes user-defined hosts`() {
    val config =
      V2rayConfig(
        log = V2rayConfig.Log(),
        inbounds = arrayListOf(),
        outbounds = arrayListOf(),
        routing = V2rayConfig.Routing(domainStrategy = "", rules = arrayListOf()),
      )

    createStep(settings = createMockSettings(userHosts = "example.com:127.0.0.1")).applyDns(config)

    assertTrue(config.dns?.hosts != null, "hosts should be set")
    assertTrue(config.dns!!.hosts!!.containsKey("example.com"), "user host should be present")
  }

  @Test
  fun `applyDns - sets refresh interval when configured`() {
    val config =
      V2rayConfig(
        log = V2rayConfig.Log(),
        inbounds = arrayListOf(),
        outbounds = arrayListOf(),
        dns = V2rayConfig.Dns(servers = arrayListOf()),
        routing = V2rayConfig.Routing(domainStrategy = "", rules = arrayListOf()),
      )

    createStep(settings = createMockSettings(dnsRefreshInterval = 300)).applyDns(config)

    assertEquals(
      300L,
      config.dns!!.refreshInterval,
      "refresh interval should be set to configured value",
    )
  }

  @Test
  fun `applyDns - does not set refresh interval when zero`() {
    val config =
      V2rayConfig(
        log = V2rayConfig.Log(),
        inbounds = arrayListOf(),
        outbounds = arrayListOf(),
        dns = V2rayConfig.Dns(servers = arrayListOf()),
        routing = V2rayConfig.Routing(domainStrategy = "", rules = arrayListOf()),
      )

    createStep(settings = createMockSettings(dnsRefreshInterval = 0)).applyDns(config)

    assertNull(
      config.dns!!.refreshInterval,
      "refresh interval should be null when configured as zero",
    )
  }

  @Test
  fun `applyCustomLocalDns - adds fakedns server when fake DNS enabled`() {
    val config =
      V2rayConfig(
        log = V2rayConfig.Log(),
        inbounds = arrayListOf(),
        outbounds = arrayListOf(),
        dns = V2rayConfig.Dns(servers = arrayListOf()),
        routing = V2rayConfig.Routing(domainStrategy = "", rules = arrayListOf()),
      )

    createStep(settings = createMockSettings(fakeDnsEnabled = true)).applyCustomLocalDns(config)

    val fakednsServer =
      config.dns?.servers?.firstOrNull {
        it is V2rayConfig.Dns.Servers && (it as V2rayConfig.Dns.Servers).address == "fakedns"
      }
    assertNotNull(fakednsServer, "fakedns server should be added when fake DNS is enabled")
  }

  @Test
  fun `applyCustomLocalDns - does not add fakedns server when fake DNS disabled`() {
    val config =
      V2rayConfig(
        log = V2rayConfig.Log(),
        inbounds = arrayListOf(),
        outbounds = arrayListOf(),
        dns = V2rayConfig.Dns(servers = arrayListOf()),
        routing = V2rayConfig.Routing(domainStrategy = "", rules = arrayListOf()),
      )

    createStep(settings = createMockSettings(fakeDnsEnabled = false)).applyCustomLocalDns(config)

    val fakednsServer =
      config.dns?.servers?.firstOrNull {
        it is V2rayConfig.Dns.Servers && (it as V2rayConfig.Dns.Servers).address == "fakedns"
      }
    assertNull(fakednsServer, "fakedns server should not be added when fake DNS is disabled")
  }

  @Test
  fun `applyCustomLocalDns - adds socks rule for HEV TUN VPN mode`() {
    val config =
      V2rayConfig(
        log = V2rayConfig.Log(),
        inbounds = arrayListOf(),
        outbounds = arrayListOf(),
        routing = V2rayConfig.Routing(domainStrategy = "", rules = arrayListOf()),
      )

    createStep(settings = createMockSettings(vpnMode = true, usingHevTun = true)).applyCustomLocalDns(config)

    val socksRule = config.routing.rules.firstOrNull { it.inboundTag?.contains("socks") == true }
    assertNotNull(socksRule, "socks rule should be added for HEV TUN VPN mode")
    assertEquals("dns-out", socksRule?.outboundTag)
    assertEquals("53", socksRule?.port)
  }

  @Test
  fun `applyCustomLocalDns - adds tun rule for regular TUN VPN mode`() {
    val config =
      V2rayConfig(
        log = V2rayConfig.Log(),
        inbounds = arrayListOf(),
        outbounds = arrayListOf(),
        routing = V2rayConfig.Routing(domainStrategy = "", rules = arrayListOf()),
      )

    createStep(settings = createMockSettings(vpnMode = true, usingHevTun = false)).applyCustomLocalDns(config)

    val tunRule = config.routing.rules.firstOrNull { it.inboundTag?.contains("tun") == true }
    assertNotNull(tunRule, "tun rule should be added for regular TUN VPN mode")
    assertEquals("dns-out", tunRule?.outboundTag)
    assertEquals("53", tunRule?.port)
  }

  @Test
  fun `applyCustomLocalDns - does not add VPN rules when not in VPN mode`() {
    val config =
      V2rayConfig(
        log = V2rayConfig.Log(),
        inbounds = arrayListOf(),
        outbounds = arrayListOf(),
        routing = V2rayConfig.Routing(domainStrategy = "", rules = arrayListOf()),
      )

    createStep(settings = createMockSettings(vpnMode = false)).applyCustomLocalDns(config)

    val vpnRules = config.routing.rules.filter { it.outboundTag == "dns-out" }
    assertTrue(vpnRules.isEmpty(), "no VPN DNS rules should be added when not in VPN mode")
  }

  @Test
  fun `applyCustomLocalDns - adds dns-out outbound when missing`() {
    val config =
      V2rayConfig(
        log = V2rayConfig.Log(),
        inbounds = arrayListOf(),
        outbounds = arrayListOf(),
        routing = V2rayConfig.Routing(domainStrategy = "", rules = arrayListOf()),
      )

    createStep().applyCustomLocalDns(config)

    val dnsOutbound = config.outbounds.firstOrNull { it.tag == "dns-out" && it.protocol == "dns" }
    assertNotNull(dnsOutbound, "dns-out outbound should be added when missing")
  }

  @Test
  fun `applyCustomLocalDns - does not duplicate dns-out outbound`() {
    val existingOutbound = V2rayConfig.Outbound(protocol = "dns", tag = "dns-out", settings = null, streamSettings = null, mux = null)
    val config =
      V2rayConfig(
        log = V2rayConfig.Log(),
        inbounds = arrayListOf(),
        outbounds = arrayListOf(existingOutbound),
        routing = V2rayConfig.Routing(domainStrategy = "", rules = arrayListOf()),
      )

    createStep().applyCustomLocalDns(config)

    val dnsOutbounds = config.outbounds.filter { it.tag == "dns-out" && it.protocol == "dns" }
    assertEquals(1, dnsOutbounds.size, "dns-out outbound should not be duplicated")
  }
}
