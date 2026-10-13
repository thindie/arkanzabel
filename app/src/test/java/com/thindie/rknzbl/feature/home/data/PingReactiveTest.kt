package com.thindie.rknzbl.feature.home.data

import com.thindie.engine.core.WorkState
import com.thindie.rknzbl.appfeatures.home.data.ConnectionProfileRepositoryImpl
import com.thindie.rknzbl.appfeatures.home.data.ProfileHttpGateway
import com.thindie.rknzbl.appfeatures.home.data.VpnServiceGateway
import com.v2ray.ang.dto.ConnectionProfile
import com.v2ray.ang.enums.Protocol
import com.v2ray.ang.runtime.KeyValueStorage
import com.v2ray.ang.util.JsonUtil
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PingReactiveTest {
  private val httpGateway = mockk<ProfileHttpGateway>()
  private val vpnGateway = mockk<VpnServiceGateway>(relaxed = true)

  @BeforeTest
  fun setUp() {
    mockkObject(KeyValueStorage)
    every { KeyValueStorage.getSelectServer() } returns null
    every { KeyValueStorage.decodeServerList() } returns emptyList()
    every { KeyValueStorage.setLocalProfiles(any()) } just Runs
    every { KeyValueStorage.setLastAutoSaveProfilesJson(any()) } just Runs
    every { KeyValueStorage.decodeSettingsBool(any(), any()) } returns false
    every { KeyValueStorage.decodeWebDavConfig() } returns null
    coEvery { httpGateway.writeWebDav(any()) } just Runs
    coEvery { httpGateway.readWebDav() } returns ""
    coEvery { httpGateway.fetchSource(any()) } returns ""
    coEvery { httpGateway.ping(any()) } answers { firstArg().copy(delay = 42L) }
    every { vpnGateway.serviceState } returns MutableStateFlow<WorkState>(WorkState.Running)
  }

  @AfterTest
  fun tearDown() = io.mockk.unmockkAll()

  private fun profile(id: String) = ConnectionProfile(protocol = Protocol.Vless, subscriptionId = id, server = "srv-$id")

  @Test
  fun `received re-emits with delay after ping`() =
    runTest {
      val url = "https://example.com/sub"
      val repository =
        ConnectionProfileRepositoryImpl(
          storage = KeyValueStorage,
          httpGateway = httpGateway,
          vpnGateway = vpnGateway,
        )
      every { KeyValueStorage.isLocalSaveEnabled() } returns false
      every { KeyValueStorage.getCustomSourceUrl() } returns url
      coEvery { httpGateway.fetchSource(url) } returns json(profile("a"))

      val before = repository.received.first()
      assertNull(before?.firstOrNull()?.delay, "expected no delay before ping")

      repository.ping()

      val after = repository.received.first()
      assertEquals(42L, after?.firstOrNull()?.delay, "received must re-emit with measured delay after ping")
    }

  private fun json(vararg profiles: ConnectionProfile): String = profiles.joinToString(separator = "########") { JsonUtil.toJson(it) }
}
