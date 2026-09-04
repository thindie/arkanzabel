package com.v2ray.ang.runtime

import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.ConnectionProfile
import com.v2ray.ang.dto.V2rayConfig.Outbound.StreamSettings
import com.v2ray.ang.enums.Protocol
import com.v2ray.ang.enums.Security
import com.v2ray.ang.util.JsonUtil
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Regression guard for the class of bug that broke every TLS/Reality outbound this session:
 * `StreamSettings.TlsSettings.allowInsecure` was a non-nullable `Boolean = false`, so Gson
 * always serialized it, and Xray-core hard-rejects the mere presence of that removed key.
 * These tests assert on the actual serialized JSON - not just the DTO - because a
 * valid-looking JSON silently rejected by native core is exactly the failure mode a
 * DTO-only assertion would miss.
 */
class V2rayConfigManagerTest {
  @BeforeTest
  fun setUp() {
    mockkObject(KeyValueStorage)
    every { KeyValueStorage.decodeSettingsBool(AppConfig.PREF_REALITY_SHOW_ENABLED, any()) } returns false
  }

  @AfterTest
  fun tearDown() = unmockkAll()

  private fun tlsProfile() =
    ConnectionProfile(
      protocol = Protocol.Vless,
      subscriptionId = "test",
      security = Security.TLS,
      sni = "example.com",
      fingerPrint = "chrome",
      pinnedCA256 = "AABBCC",
      alpn = "h2,http/1.1",
    )

  private fun realityProfile() =
    ConnectionProfile(
      protocol = Protocol.Vless,
      subscriptionId = "test",
      security = Security.REALITY,
      sni = "www.microsoft.com",
      fingerPrint = "chrome",
      publicKey = "reality-public-key",
      shortId = "abcd1234",
      spiderX = "/",
    )

  @Test
  fun `TLS security populates tlsSettings, not realitySettings, and never emits allowInsecure`() {
    val streamSettings = StreamSettings()

    V2rayConfigManager.populateTlsSettings(streamSettings, tlsProfile(), sniExt = null)

    assertTrue(streamSettings.tlsSettings != null)
    assertNull(streamSettings.realitySettings)

    val json = JsonUtil.toJson(streamSettings)
    assertFalse(json.contains("allowInsecure"), "allowInsecure must never be serialized - Xray-core rejects its mere presence")
    assertTrue(json.contains("\"serverName\":\"example.com\""))
    assertTrue(json.contains("\"fingerprint\":\"chrome\""))
    assertTrue(json.contains("\"pinnedPeerCertSha256\":\"AABBCC\""))
  }

  @Test
  fun `Reality security populates realitySettings, not tlsSettings, and never emits allowInsecure`() {
    val streamSettings = StreamSettings()

    V2rayConfigManager.populateTlsSettings(streamSettings, realityProfile(), sniExt = null)

    assertTrue(streamSettings.realitySettings != null)
    assertNull(streamSettings.tlsSettings)

    val json = JsonUtil.toJson(streamSettings)
    assertFalse(json.contains("allowInsecure"), "allowInsecure must never be serialized - Xray-core rejects its mere presence")
    assertTrue(json.contains("\"publicKey\":\"reality-public-key\""))
    // realityPublicKeyPassword is @SerializedName("password") - the Reality client alias for pbk.
    assertTrue(json.contains("\"password\":\"reality-public-key\""))
    assertTrue(json.contains("\"shortId\":\"abcd1234\""))
  }

  @Test
  fun `blank security leaves streamSettings untouched`() {
    val streamSettings = StreamSettings()
    val profile = tlsProfile().copy(security = null)

    V2rayConfigManager.populateTlsSettings(streamSettings, profile, sniExt = null)

    assertNull(streamSettings.security)
    assertNull(streamSettings.tlsSettings)
    assertNull(streamSettings.realitySettings)
  }
}
