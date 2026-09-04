package com.v2ray.ang.runtimebuilder

import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.RulesetItem
import com.v2ray.ang.dto.V2rayConfig
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RoutingConfigStepTest {
  private fun emptyConfig() =
    V2rayConfig(
      log = V2rayConfig.Log(),
      inbounds = arrayListOf(),
      outbounds = arrayListOf(),
      routing = V2rayConfig.Routing(domainStrategy = "", rules = arrayListOf()),
    )

  private fun createSettings(
    domainStrategy: String? = null,
    rulesets: List<RulesetItem>? = null,
  ) = mockk<SettingsReader>(relaxed = true).also {
    every { it.getString(AppConfig.PREF_ROUTING_DOMAIN_STRATEGY) } returns domainStrategy
    every { it.getRoutingRulesets() } returns rulesets
  }

  @Test
  fun `applyRouting - falls back to AsIs when no strategy stored`() {
    val step = RoutingConfigStep(createSettings())

    val result = step.applyRouting(emptyConfig())

    assertEquals("AsIs", result.routing.domainStrategy)
  }

  @Test
  fun `applyRouting - uses stored domain strategy`() {
    val step = RoutingConfigStep(createSettings(domainStrategy = "IPOnDemand"))

    val result = step.applyRouting(emptyConfig())

    assertEquals("IPOnDemand", result.routing.domainStrategy)
  }

  @Test
  fun `applyRouting - adds only enabled rulesets as routing rules`() {
    val rulesets =
      listOf(
        RulesetItem(remarks = "enabled rule", outboundTag = "proxy", domain = listOf("domain:example.com"), enabled = true),
        RulesetItem(remarks = "disabled rule", outboundTag = "direct", domain = listOf("domain:skip.com"), enabled = false),
      )
    val step = RoutingConfigStep(createSettings(rulesets = rulesets))

    val result = step.applyRouting(emptyConfig())

    assertEquals(1, result.routing.rules.size)
    assertEquals("proxy", result.routing.rules.first().outboundTag)
  }

  @Test
  fun `getUserRule2Domain - collects only geosite and domain entries for the matching tag`() {
    val rulesets =
      listOf(
        RulesetItem(
          remarks = "proxy rule",
          outboundTag = "proxy",
          domain = listOf("geosite:google", "domain:example.com", "full:literal.com", AppConfig.GEOSITE_PRIVATE),
          enabled = true,
        ),
        RulesetItem(remarks = "other tag", outboundTag = "direct", domain = listOf("domain:other.com"), enabled = true),
      )
    val step = RoutingConfigStep(createSettings(rulesets = rulesets))

    val domains = step.getUserRule2Domain("proxy")

    assertEquals(listOf("geosite:google", "domain:example.com"), domains)
    assertTrue(AppConfig.GEOSITE_PRIVATE !in domains)
    assertTrue("full:literal.com" !in domains)
  }
}
