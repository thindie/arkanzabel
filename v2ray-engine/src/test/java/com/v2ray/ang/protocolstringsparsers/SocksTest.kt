package com.v2ray.ang.protocolstringsparsers

import com.v2ray.ang.enums.Protocol
import com.v2ray.ang.testutil.mockAndroidFrameworkStatics
import com.v2ray.ang.util.Utils
import io.mockk.unmockkAll
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SocksTest {
  @BeforeTest
  fun setUp() = mockAndroidFrameworkStatics()

  @AfterTest
  fun tearDown() = unmockkAll()

  @Test
  fun `parse - with credentials`() {
    val userInfo = Utils.encode("user:pass", removePadding = true)

    val profile = requireNotNull(Socks.parse("socks://$userInfo@1.2.3.4:1080#My%20Socks"))

    assertEquals(Protocol.Socks, profile.protocol)
    assertEquals("1.2.3.4", profile.server)
    assertEquals("1080", profile.serverPort)
    assertEquals("user", profile.username)
    assertEquals("pass", profile.password)
  }

  @Test
  fun `parse - no credentials`() {
    val profile = requireNotNull(Socks.parse("socks://1.2.3.4:1080#Open%20Proxy"))

    assertNull(profile.username)
    assertNull(profile.password)
  }

  @Test
  fun `parse - missing port returns null`() {
    assertNull(Socks.parse("socks://1.2.3.4#x"))
  }
}
