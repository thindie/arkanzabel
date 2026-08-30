package com.v2ray.ang.runtimebuilder

import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.ConnectionProfile
import com.v2ray.ang.dto.V2rayConfig
import com.v2ray.ang.enums.Protocol
import com.v2ray.ang.error.ConfigBuildError
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ConfigAssemblerTest {
  private fun createMockSettings(
    speedEnabled: Boolean = false,
    localDnsEnabled: Boolean = false,
    domainResolveMethod: String = "0",
  ) = mockk<SettingsReader>(relaxed = true).also {
    every { it.getBool(AppConfig.PREF_SPEED_ENABLED, any()) } returns speedEnabled
    every { it.getBool(AppConfig.PREF_LOCAL_DNS_ENABLED, any()) } returns localDnsEnabled
    every { it.getString(AppConfig.PREF_OUTBOUND_DOMAIN_RESOLVE_METHOD, any()) } returns domainResolveMethod
  }

  private fun createAssembler(settings: SettingsReader = createMockSettings()) =
    ConfigAssembler(
      settings = settings,
      applyInbounds = { config -> config },
      applyOutbounds = { config, _ -> config },
      applyMoreOutbounds = { config, _ -> config },
      applyRouting = { config -> config },
      applyFakeDns = { config -> config },
      applyDns = { config -> config },
      applyCustomLocalDns = { config -> config },
      applyResolveOutboundDomainsToHosts = { throw RuntimeException("Should not be called") },
    )

  private fun createProfile(subscriptionId: String = "") = ConnectionProfile(protocol = Protocol.Vless, subscriptionId = subscriptionId)

  @Test
  fun `applyStandardSteps - speed disabled sets stats and policy to null`() {
    val config =
      V2rayConfig(
        stats = Any(),
        policy = V2rayConfig.Policy(levels = emptyMap()),
        log = V2rayConfig.Log(),
        inbounds = arrayListOf(),
        outbounds = arrayListOf(),
        routing = V2rayConfig.Routing(domainStrategy = "", rules = arrayListOf()),
      )

    val result =
      createAssembler(settings = createMockSettings(speedEnabled = false))
        .applyStandardSteps(config, createProfile())

    assertNull(result.stats, "stats should be null when speed is disabled")
    assertNull(result.policy, "policy should be null when speed is disabled")
  }

  @Test
  fun `applyStandardSteps - speed enabled preserves stats and policy`() {
    val stats = Any()
    val policy = V2rayConfig.Policy(levels = emptyMap())
    val config =
      V2rayConfig(
        stats = stats,
        policy = policy,
        log = V2rayConfig.Log(),
        inbounds = arrayListOf(),
        outbounds = arrayListOf(),
        routing = V2rayConfig.Routing(domainStrategy = "", rules = arrayListOf()),
      )

    val result =
      createAssembler(settings = createMockSettings(speedEnabled = true))
        .applyStandardSteps(config, createProfile())

    assertEquals(stats, result.stats, "stats should be preserved when speed is enabled")
    assertEquals(policy, result.policy, "policy should be preserved when speed is enabled")
  }

  @Test
  fun `applyStandardSteps - domain resolve disabled by default`() {
    val config =
      V2rayConfig(
        log = V2rayConfig.Log(),
        inbounds = arrayListOf(),
        outbounds = arrayListOf(),
        routing = V2rayConfig.Routing(domainStrategy = "", rules = arrayListOf()),
      )

    var resolveCalled = false
    val assembler =
      ConfigAssembler(
        settings = createMockSettings(domainResolveMethod = "0"),
        applyInbounds = { config -> config },
        applyOutbounds = { config, _ -> config },
        applyMoreOutbounds = { config, _ -> config },
        applyRouting = { config -> config },
        applyFakeDns = { config -> config },
        applyDns = { config -> config },
        applyCustomLocalDns = { config -> config },
        applyResolveOutboundDomainsToHosts = { config ->
          resolveCalled = true
          config
        },
      )

    assembler.applyStandardSteps(config, createProfile())

    assertEquals(false, resolveCalled, "resolve should not be called when method is '0'")
  }

  @Test
  fun `applyStandardSteps - domain resolve enabled calls resolver`() {
    val config =
      V2rayConfig(
        log = V2rayConfig.Log(),
        inbounds = arrayListOf(),
        outbounds = arrayListOf(),
        routing = V2rayConfig.Routing(domainStrategy = "", rules = arrayListOf()),
      )

    var resolveCalled = false
    val assembler =
      ConfigAssembler(
        settings = createMockSettings(domainResolveMethod = "1"),
        applyInbounds = { config -> config },
        applyOutbounds = { config, _ -> config },
        applyMoreOutbounds = { config, _ -> config },
        applyRouting = { config -> config },
        applyFakeDns = { config -> config },
        applyDns = { config -> config },
        applyCustomLocalDns = { config -> config },
        applyResolveOutboundDomainsToHosts = { config ->
          resolveCalled = true
          config
        },
      )

    assembler.applyStandardSteps(config, createProfile())

    assertEquals(true, resolveCalled, "resolve should be called when method is '1'")
  }

  @Test
  fun `applyStandardSteps - domain resolve throws ConfigBuildError on failure`() {
    val config =
      V2rayConfig(
        log = V2rayConfig.Log(),
        inbounds = arrayListOf(),
        outbounds = arrayListOf(),
        routing = V2rayConfig.Routing(domainStrategy = "", rules = arrayListOf()),
      )

    val assembler =
      ConfigAssembler(
        settings = createMockSettings(domainResolveMethod = "1"),
        applyInbounds = { config -> config },
        applyOutbounds = { config, _ -> config },
        applyMoreOutbounds = { config, _ -> config },
        applyRouting = { config -> config },
        applyFakeDns = { config -> config },
        applyDns = { config -> config },
        applyCustomLocalDns = { config -> config },
        applyResolveOutboundDomainsToHosts = { throw RuntimeException("DNS failed") },
      )

    assertFailsWith<ConfigBuildError> {
      assembler.applyStandardSteps(config, createProfile())
    }
  }

  @Test
  fun `applyStandardSteps - passes subscriptionId to more outbounds`() {
    val config =
      V2rayConfig(
        log = V2rayConfig.Log(),
        inbounds = arrayListOf(),
        outbounds = arrayListOf(),
        routing = V2rayConfig.Routing(domainStrategy = "", rules = arrayListOf()),
      )

    var capturedSubId: String? = null
    val assembler =
      ConfigAssembler(
        settings = createMockSettings(),
        applyInbounds = { config -> config },
        applyOutbounds = { config, _ -> config },
        applyMoreOutbounds = { config, subId ->
          capturedSubId = subId
          config
        },
        applyRouting = { config -> config },
        applyFakeDns = { config -> config },
        applyDns = { config -> config },
        applyCustomLocalDns = { config -> config },
        applyResolveOutboundDomainsToHosts = { config -> config },
      )

    assembler.applyStandardSteps(config, createProfile("sub-123"))

    assertEquals("sub-123", capturedSubId, "subscriptionId should be passed to more outbounds")
  }
}
