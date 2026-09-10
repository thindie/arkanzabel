package com.v2ray.ang.runtimebuilder

import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.V2rayConfig
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DomainResolveStepTest {
  private fun createMockSettings(preferIpv6: Boolean = false) =
    mockk<SettingsReader>(relaxed = true).also {
      every { it.getBool(AppConfig.PREF_PREFER_IPV6, any()) } returns preferIpv6
    }

  private fun createStep(settings: SettingsReader = createMockSettings()) = DomainResolveStep(settings)

  @Test
  fun `resolveOutboundDomainsToHosts - returns unchanged when no DNS`() {
    val config =
      V2rayConfig(
        log = V2rayConfig.Log(),
        inbounds = arrayListOf(),
        outbounds = arrayListOf(V2rayConfig.Outbound(protocol = "proxy")),
        routing = V2rayConfig.Routing(domainStrategy = "", rules = arrayListOf()),
      )

    val result = createStep().resolveOutboundDomainsToHosts(config)

    assertEquals(config, result, "config should be unchanged when DNS is null")
  }

  @Test
  fun `resolveOutboundDomainsToHosts - sets domain strategy for outbound with existing host`() {
    val config =
      V2rayConfig(
        log = V2rayConfig.Log(),
        inbounds = arrayListOf(),
        outbounds =
          arrayListOf(
            V2rayConfig.Outbound(
              protocol = "proxy",
              settings =
                V2rayConfig.Outbound.OutSettings(
                  vnext =
                    listOf(
                      V2rayConfig.Outbound.OutSettings.Vnext(
                        address = "example.com",
                        port = 443,
                        users = emptyList(),
                      ),
                    ),
                ),
            ),
          ),
        dns =
          V2rayConfig.Dns(
            hosts = mutableMapOf("example.com" to "1.2.3.4"),
            servers = arrayListOf(),
          ),
        routing = V2rayConfig.Routing(domainStrategy = "", rules = arrayListOf()),
      )

    val result = createStep().resolveOutboundDomainsToHosts(config)

    assertNotNull(result.dns?.hosts, "dns hosts should be set")
    assertEquals("1.2.3.4", result.dns!!.hosts!!["example.com"], "existing host should be preserved")
  }
}
