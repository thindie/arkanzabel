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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Golden-vector characterization tests: pin current parsing behavior so a future refactor
 * (typed security/network fields, dedup with other protocol parsers) can be diffed against
 * a known-good baseline instead of guessed at.
 */
class VlessTest {
  @BeforeTest
  fun setUp() {
    mockAndroidFrameworkStatics()
    mockkObject(KeyValueStorage)
    every { KeyValueStorage.decodeSettingsBool(AppConfig.PREF_ALLOW_INSECURE, any()) } returns false
  }

  @AfterTest
  fun tearDown() = unmockkAll()

  @Test
  fun `parse - reality profile with pbk in query`() {
    val uri =
      "vless://11111111-2222-3333-4444-555555555555@1.2.3.4:443" +
        "?encryption=none&security=reality&sni=www.microsoft.com&fp=chrome" +
        "&pbk=SGVsbG9SZWFsaXR5UHVibGljS2V5&sid=abcd1234&spx=%2F&flow=xtls-rprx-vision&type=tcp" +
        "#Reality%20Server"

    val profile = Vless.parse(uri)

    assertEquals(Protocol.Vless, profile?.protocol)
    assertEquals("1.2.3.4", profile?.server)
    assertEquals("443", profile?.serverPort)
    assertEquals("11111111-2222-3333-4444-555555555555", profile?.password)
    assertEquals("none", profile?.method)
    assertEquals(Security.REALITY, profile?.security)
    assertEquals("www.microsoft.com", profile?.sni)
    assertEquals("chrome", profile?.fingerPrint)
    assertEquals("SGVsbG9SZWFsaXR5UHVibGljS2V5", profile?.publicKey)
    assertEquals("abcd1234", profile?.shortId)
    assertEquals("/", profile?.spiderX)
    assertEquals("xtls-rprx-vision", profile?.flow)
    assertEquals(NetworkType.TCP, profile?.network)
    assertEquals("Reality Server", profile?.remarks)
    assertFalse(profile!!.insecure)
  }

  @Test
  fun `parse - tls profile over websocket`() {
    val uri =
      "vless://11111111-2222-3333-4444-555555555555@example.com:443" +
        "?encryption=none&security=tls&sni=example.com&fp=chrome&type=ws&host=example.com&path=%2Fws" +
        "#My%20Server"

    val profile = Vless.parse(uri)

    assertEquals(Security.TLS, profile?.security)
    assertEquals(NetworkType.WS, profile?.network)
    assertEquals("example.com", profile?.host)
    assertEquals("/ws", profile?.path)
    assertNull(profile?.publicKey)
  }

  @Test
  fun `parse - no query string returns null`() {
    // Current behavior: Vless requires a query string (unlike Trojan/Hysteria2, which build
    // a profile with TLS defaults instead). This is a real divergence between parsers -
    // pinned here as-is, not declared "correct".
    val uri = "vless://11111111-2222-3333-4444-555555555555@example.com:443#My%20Server"

    assertNull(Vless.parse(uri))
  }

  @Test
  fun `parse - unrecognized security value is dropped, not passed through raw`() {
    val uri = "vless://11111111-2222-3333-4444-555555555555@example.com:443?security=garbage#x"

    val profile = Vless.parse(uri)

    assertNull(profile?.security)
  }

  @Test
  fun `parse - insecure query flag overrides settings default`() {
    every { KeyValueStorage.decodeSettingsBool(AppConfig.PREF_ALLOW_INSECURE, any()) } returns false
    val uriInsecure1 = "vless://uuid@example.com:443?security=tls&insecure=1#x"
    val uriInsecure0 = "vless://uuid@example.com:443?security=tls&allowInsecure=0#x"

    assertTrue(Vless.parse(uriInsecure1)!!.insecure)
    assertFalse(Vless.parse(uriInsecure0)!!.insecure)
  }

  @Test
  fun `parse - falls back to settings default when query omits insecure flag`() {
    every { KeyValueStorage.decodeSettingsBool(AppConfig.PREF_ALLOW_INSECURE, any()) } returns true
    val uri = "vless://uuid@example.com:443?security=tls#x"

    assertTrue(Vless.parse(uri)!!.insecure)
  }

  @Test
  fun `toUri round-trips server, port, security and reality fields`() {
    val original =
      "vless://11111111-2222-3333-4444-555555555555@1.2.3.4:443" +
        "?encryption=none&security=reality&sni=www.microsoft.com&fp=chrome" +
        "&pbk=SGVsbG9SZWFsaXR5UHVibGljS2V5&sid=abcd1234&type=tcp#Reality%20Server"
    val profile = requireNotNull(Vless.parse(original))

    // toUri() emits "user@host:port?query#frag" with no scheme prefix - it has no
    // production caller today, so the scheme has to be added back manually to re-parse.
    val reparsed = requireNotNull(Vless.parse(AppConfig.VLESS + Vless.toUri(profile)))

    assertEquals(profile.server, reparsed.server)
    assertEquals(profile.serverPort, reparsed.serverPort)
    assertEquals(profile.password, reparsed.password)
    assertEquals(profile.security, reparsed.security)
    assertEquals(profile.sni, reparsed.sni)
    assertEquals(profile.fingerPrint, reparsed.fingerPrint)
    assertEquals(profile.publicKey, reparsed.publicKey)
    assertEquals(profile.shortId, reparsed.shortId)
  }
}
