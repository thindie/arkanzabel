package com.thindie.rknzbl.feature.home.data

import com.thindie.rknzbl.application.ProfilePingManager
import com.v2ray.ang.dto.ConnectionProfile
import com.v2ray.ang.enums.Protocol
import com.v2ray.ang.runtime.KeyValueStorage
import com.v2ray.ang.util.JsonUtil
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConnectionProfileRepositoryImplTest {
  private val pingManager = mockk<ProfilePingManager>(relaxed = true)
  private val httpGateway = mockk<ProfileHttpGateway>()
  private val vpnGateway = mockk<VpnServiceGateway>(relaxed = true)

  @BeforeTest
  fun setUp() {
    // mockkObject spies the singleton: unstubbed calls fall through to the real implementation,
    // so side-effecting methods must be stubbed explicitly (they would otherwise hit MMKV).
    mockkObject(KeyValueStorage)
    every { KeyValueStorage.getSelectServer() } returns null
    every { KeyValueStorage.decodeServerList() } returns emptyList()
    every { KeyValueStorage.setLocalProfiles(any()) } just Runs
    every { KeyValueStorage.setLastAutoSaveProfilesJson(any()) } just Runs
    every { KeyValueStorage.decodeSettingsBool(any(), any()) } returns false
    coEvery { httpGateway.writeWebDav(any()) } just Runs
  }

  @AfterTest
  fun tearDown() = unmockkAll()

  private fun createRepository(
    localSave: Boolean,
    sourceUrl: String? = null,
  ): ConnectionProfileRepositoryImpl {
    every { KeyValueStorage.isLocalSaveEnabled() } returns localSave
    every { KeyValueStorage.getCustomSourceUrl() } returns sourceUrl
    return ConnectionProfileRepositoryImpl(
      pingManager = pingManager,
      storage = KeyValueStorage,
      httpGateway = httpGateway,
      vpnGateway = vpnGateway,
    )
  }

  // server is part of ConnectionProfile's custom equals (subscriptionId is not), so profiles
  // must differ in it to survive the repository's dedup via toSet().
  private fun profile(id: String) = ConnectionProfile(protocol = Protocol.Vless, subscriptionId = id, server = "srv-$id")

  // read()

  @Test
  fun `read local save parses stored body and caches it`() =
    runTest {
      val repository = createRepository(localSave = true)
      val p1 = profile("a")
      val p2 = profile("b")
      every { KeyValueStorage.getLocalProfiles() } returns json(p1, p2)

      assertEquals(listOf(p1, p2), repository.read())
      // Second read is served from the in-memory cache.
      assertEquals(listOf(p1, p2), repository.read())
      verify(exactly = 1) { KeyValueStorage.getLocalProfiles() }
    }

  @Test
  fun `read local save with empty body returns empty list`() =
    runTest {
      val repository = createRepository(localSave = true)
      every { KeyValueStorage.getLocalProfiles() } returns null

      assertTrue(repository.read().isEmpty())
    }

  @Test
  fun `read remote without source url returns empty list and skips network`() =
    runTest {
      val repository = createRepository(localSave = false, sourceUrl = null)

      assertTrue(repository.read().isEmpty())
      coVerify(exactly = 0) { httpGateway.fetchSource(any()) }
    }

  @Test
  fun `read remote fetches and parses the custom source`() =
    runTest {
      val url = "https://example.com/sub"
      val repository = createRepository(localSave = false, sourceUrl = url)
      val p1 = profile("a")
      val p2 = profile("b")
      coEvery { httpGateway.fetchSource(url) } returns "${json(p1)}\n${json(p2)}"

      assertEquals(listOf(p1, p2), repository.read())
      coVerify(exactly = 1) { httpGateway.fetchSource(url) }
    }

  // save()

  @Test
  fun `save with blank guid or undecodable profile returns false`() =
    runTest {
      val repository = createRepository(localSave = true)
      every { KeyValueStorage.decodeServerConfig("bad") } returns null

      assertFalse(repository.save(""))
      assertFalse(repository.save("bad"))
      verify(exactly = 0) { KeyValueStorage.setLocalProfiles(any()) }
    }

  @Test
  fun `save local appends new profile and updates stored cache`() =
    runTest {
      val repository = createRepository(localSave = true)
      val p = profile("new")
      every { KeyValueStorage.decodeServerConfig("g1") } returns p
      every { KeyValueStorage.getLocalProfiles() } returns ""

      assertTrue(repository.save("g1"))

      val written = slot<String>()
      verify(exactly = 1) { KeyValueStorage.setLocalProfiles(capture(written)) }
      assertEquals(SEP + json(p), written.captured)
      assertEquals(listOf(p), repository.stored.first())
    }

  @Test
  fun `save local returns false when profile is already stored`() =
    runTest {
      val repository = createRepository(localSave = true)
      val p = profile("dup")
      every { KeyValueStorage.decodeServerConfig("g1") } returns p
      every { KeyValueStorage.getLocalProfiles() } returns json(p)

      assertFalse(repository.save("g1"))
      verify(exactly = 0) { KeyValueStorage.setLocalProfiles(any()) }
    }

  @Test
  fun `save webdav returns false when profile is already stored remotely`() =
    runTest {
      val repository = createRepository(localSave = false, sourceUrl = "https://example.com/dav")
      val p = profile("dup")
      every { KeyValueStorage.decodeServerConfig("g1") } returns p
      coEvery { httpGateway.readWebDav() } returns json(p)

      assertFalse(repository.save("g1"))
      coVerify(exactly = 0) { httpGateway.writeWebDav(any()) }
    }

  @Test
  fun `save webdav appends to remote body instead of local storage`() =
    runTest {
      val repository = createRepository(localSave = false, sourceUrl = "https://example.com/dav")
      val p = profile("new")
      every { KeyValueStorage.decodeServerConfig("g1") } returns p
      coEvery { httpGateway.readWebDav() } returns ""

      assertTrue(repository.save("g1"))

      val written = slot<String>()
      coVerify(exactly = 1) { httpGateway.writeWebDav(capture(written)) }
      assertEquals(SEP + json(p), written.captured)
      verify(exactly = 0) { KeyValueStorage.setLocalProfiles(any()) }
    }

  @Test
  fun `switching storage mode at runtime switches the stored cache key`() =
    runTest {
      val repository = createRepository(localSave = true)
      val p1 = profile("local")
      every { KeyValueStorage.getLocalProfiles() } returns json(p1)
      repository.read()
      assertEquals(listOf(p1), repository.stored.first())

      // Flip to webdav mode at runtime: the stored view must follow the new cache key.
      every { KeyValueStorage.isLocalSaveEnabled() } returns false
      every { KeyValueStorage.getCustomSourceUrl() } returns "https://example.com/dav"
      assertNull(repository.stored.first())

      val p2 = profile("dav")
      every { KeyValueStorage.decodeServerConfig("g1") } returns p2
      coEvery { httpGateway.readWebDav() } returns ""
      assertTrue(repository.save("g1"))
      assertEquals(listOf(p2), repository.stored.first())
    }

  // delete()

  @Test
  fun `delete local removes profile and collapses double separators`() =
    runTest {
      val repository = createRepository(localSave = true)
      val p1 = profile("a")
      val p2 = profile("b")
      val p3 = profile("c")
      every { KeyValueStorage.getLocalProfiles() } returns json(p1, p2, p3)

      repository.delete(p2)

      val written = slot<String>()
      verify(exactly = 1) { KeyValueStorage.setLocalProfiles(capture(written)) }
      assertEquals(json(p1, p3), written.captured)
      assertEquals(listOf(p1, p3), repository.stored.first())
    }

  @Test
  fun `delete webdav rewrites remote body and skips local storage`() =
    runTest {
      val repository = createRepository(localSave = false, sourceUrl = "https://example.com/dav")
      val p1 = profile("a")
      val p2 = profile("b")
      coEvery { httpGateway.readWebDav() } returns json(p1, p2)

      repository.delete(p1)

      val written = slot<String>()
      coVerify(exactly = 1) { httpGateway.writeWebDav(capture(written)) }
      assertEquals(SEP + json(p2), written.captured)
      verify(exactly = 0) { KeyValueStorage.setLocalProfiles(any()) }
    }

  // fetch / flows

  @Test
  fun `fetchFromSource serves cache hit without network`() =
    runTest {
      val url = "https://example.com/sub"
      val repository = createRepository(localSave = false, sourceUrl = url)
      val p1 = profile("a")
      coEvery { httpGateway.fetchSource(url) } returns json(p1)

      assertEquals(listOf(p1), repository.fetchFromSource(url))
      assertEquals(listOf(p1), repository.fetchFromSource(url))
      coVerify(exactly = 1) { httpGateway.fetchSource(url) }
    }

  @Test
  fun `fetch with force refetches and pings profiles`() =
    runTest {
      val url = "https://example.com/sub"
      val repository = createRepository(localSave = false, sourceUrl = url)
      val p1 = profile("a")
      val p2 = profile("b")
      coEvery { httpGateway.fetchSource(url) } returns json(p1) andThen json(p2)

      repository.fetch(force = false)
      repository.fetch(force = true)

      coVerify(exactly = 2) { httpGateway.fetchSource(url) }
      verify(exactly = 1) { pingManager.pingProfiles(listOf(p1), force = false) }
      verify(exactly = 1) { pingManager.pingProfiles(listOf(p2), force = false) }
    }

  @Test
  fun `fetch without source url is a no-op`() =
    runTest {
      val repository = createRepository(localSave = true, sourceUrl = null)

      repository.fetch(force = true)

      coVerify(exactly = 0) { httpGateway.fetchSource(any()) }
    }

  @Test
  fun `received flow exposes the active source cache`() =
    runTest {
      val url = "https://example.com/sub"
      val repository = createRepository(localSave = false, sourceUrl = url)
      val p1 = profile("a")
      coEvery { httpGateway.fetchSource(url) } returns json(p1)

      assertNull(repository.received.first())
      repository.fetchFromSource(url)
      assertEquals(listOf(p1), repository.received.first())
    }

  @Test
  fun `stored flow reflects saved profiles`() =
    runTest {
      val repository = createRepository(localSave = true)
      every { KeyValueStorage.getLocalProfiles() } returns null

      assertNull(repository.stored.first())
      repository.read()
      assertEquals(emptyList<ConnectionProfile>(), repository.stored.first())
    }

  // lastMeasured()

  @Test
  fun `lastMeasured emits the best profile of the completed batch`() =
    runTest {
      // The repository captures pingManager.batch at construction, so stub it first.
      val batchFlow = MutableStateFlow<ProfilePingManager.BatchResult?>(null)
      every { pingManager.batch } returns batchFlow
      val repository = createRepository(localSave = true)
      val fast = profile("fast")
      val slow = profile("slow")
      val failed = profile("failed")

      assertNull(repository.lastMeasured.first())
      batchFlow.value = ProfilePingManager.BatchResult(1L, mapOf(fast to 20L, slow to 80L, failed to -1L))
      assertEquals(fast, repository.lastMeasured.first())
    }

  @Test
  fun `lastMeasured is null when the batch has no successful measurements`() =
    runTest {
      // The repository captures pingManager.batch at construction, so stub it first.
      val batchFlow = MutableStateFlow<ProfilePingManager.BatchResult?>(null)
      every { pingManager.batch } returns batchFlow
      val repository = createRepository(localSave = true)
      val failed = profile("failed")
      val unmeasured = profile("unmeasured")

      batchFlow.value = ProfilePingManager.BatchResult(1L, mapOf(failed to -1L, unmeasured to 0L))
      assertNull(repository.lastMeasured.first())
    }

  @Test
  fun `invalidateCaches forces a re-read from storage`() =
    runTest {
      val repository = createRepository(localSave = true)
      val p1 = profile("a")
      every { KeyValueStorage.getLocalProfiles() } returns json(p1)
      repository.read()

      val p2 = profile("b")
      repository.invalidateCaches()
      every { KeyValueStorage.getLocalProfiles() } returns json(p2)

      assertEquals(listOf(p2), repository.read())
      verify(exactly = 2) { KeyValueStorage.getLocalProfiles() }
    }

  @Test
  fun `fetch without force skips network when the source cache is warm`() =
    runTest {
      val url = "https://example.com/sub"
      val repository = createRepository(localSave = false, sourceUrl = url)
      coEvery { httpGateway.fetchSource(url) } returns json(profile("a"))

      repository.fetch(force = false)
      repository.fetch(force = false)

      coVerify(exactly = 1) { httpGateway.fetchSource(url) }
    }

  @Test
  fun `fetchFromSource parses subscription uri lines and skips comments and unknown lines`() =
    runTest {
      val url = "https://example.com/sub"
      val repository = createRepository(localSave = false, sourceUrl = url)
      val uriLine =
        "vless://11111111-2222-3333-4444-555555555555@example.com:443" +
          "?encryption=none&security=tls&sni=example.com&type=ws#My%20Server"
      coEvery { httpGateway.fetchSource(url) } returns "# comment header\n$uriLine\ngarbage-not-a-profile\n"

      val profiles = repository.fetchFromSource(url)

      assertEquals(1, profiles.size)
      assertEquals("example.com", profiles.first().server)
    }

  // invalidateStoredCache / invalidateRemoteCache

  @Test
  fun `invalidateStoredCache forces a re-read of stored profiles`() =
    runTest {
      val repository = createRepository(localSave = true)
      every { KeyValueStorage.getLocalProfiles() } returns json(profile("a"))
      repository.read()

      every { KeyValueStorage.getLocalProfiles() } returns json(profile("b"))
      assertEquals(listOf(profile("a")), repository.read()) // still cached

      repository.invalidateStoredCache()
      assertEquals(listOf(profile("b")), repository.read())
    }

  @Test
  fun `invalidateRemoteCache forces a refetch from the source`() =
    runTest {
      val url = "https://example.com/sub"
      val repository = createRepository(localSave = false, sourceUrl = url)
      coEvery { httpGateway.fetchSource(url) } returns json(profile("a")) andThen json(profile("b"))

      assertEquals(listOf(profile("a")), repository.fetchFromSource(url))
      repository.invalidateRemoteCache(url)
      assertEquals(listOf(profile("b")), repository.fetchFromSource(url))
      coVerify(exactly = 2) { httpGateway.fetchSource(url) }
    }

  // isSaved / activeProfile

  @Test
  fun `isSaved matches by subscriptionId in the stored cache`() =
    runTest {
      val repository = createRepository(localSave = true)
      every { KeyValueStorage.getLocalProfiles() } returns json(profile("a"))
      repository.read()

      assertTrue(repository.isSaved(profile("a")))
      assertFalse(repository.isSaved(profile("b")))
    }

  @Test
  fun `activeProfile reads the selected server and caches it`() =
    runTest {
      val repository = createRepository(localSave = true)
      val p = profile("sel")
      every { KeyValueStorage.getSelectServer() } returns "guid-sel"
      every { KeyValueStorage.decodeServerConfig("guid-sel") } returns p

      assertEquals(p, repository.activeProfile())
      assertEquals(p, repository.connected.first())
      verify(exactly = 1) { KeyValueStorage.decodeServerConfig("guid-sel") }
    }

  @Test
  fun `activeProfile is null when nothing is selected`() =
    runTest {
      val repository = createRepository(localSave = true)

      assertNull(repository.activeProfile())
    }

  // connect / disconnect via the gateway

  @Test
  fun `connect stores unknown profile and starts service with new guid`() =
    runTest {
      val repository = createRepository(localSave = true)
      val p = profile("c1")
      val guidSlot = slot<String>()
      every { KeyValueStorage.encodeServerConfig(capture(guidSlot), p) } answers { firstArg<String>() }

      repository.connect(p)

      verify(exactly = 1) { vpnGateway.startVService(guidSlot.captured) }
      assertEquals(p, repository.connected.first())
    }

  @Test
  fun `connect reuses existing guid matched by subscriptionId`() =
    runTest {
      val repository = createRepository(localSave = true)
      val p = profile("c2")
      every { KeyValueStorage.decodeServerList() } returns listOf("guid-exists")
      every { KeyValueStorage.decodeServerConfig("guid-exists") } returns p

      repository.connect(p)

      verify(exactly = 1) { vpnGateway.startVService("guid-exists") }
      verify(exactly = 0) { KeyValueStorage.encodeServerConfig(any(), any()) }
    }

  @Test
  fun `disconnect stops service and clears connected profile`() =
    runTest {
      val repository = createRepository(localSave = true)
      val p = profile("d1")
      every { KeyValueStorage.encodeServerConfig(any(), p) } answers { firstArg<String>() }
      repository.connect(p)
      assertEquals(p, repository.connected.first())

      repository.disconnect()

      verify(exactly = 1) { vpnGateway.stopVService() }
      assertNull(repository.connected.first())
    }

  @Test
  fun `isConnected and server name delegate to the gateway`() =
    runTest {
      val repository = createRepository(localSave = true)
      every { vpnGateway.isRunning() } returns true
      every { vpnGateway.getRunningServerName() } returns "srv"

      assertTrue(repository.isConnected())
      assertEquals("srv", repository.getConnectedServerName())
    }

  // auto-save events

  @Test
  fun `saveAuto persists guid and emits the decoded profile`() =
    runTest {
      val repository = createRepository(localSave = true)
      val p = profile("auto")
      every { KeyValueStorage.decodeServerConfig("guid-auto") } returns p

      val result = CompletableDeferred<ConnectionProfile?>()
      launch(UnconfinedTestDispatcher(testScheduler)) {
        result.complete(repository.autoSaved().first())
      }
      repository.saveAuto("guid-auto")

      verify(exactly = 1) { KeyValueStorage.setLastAutoSaveProfilesJson("guid-auto") }
      assertEquals(p, withTimeout(2_000) { result.await() })
    }

  @Test
  fun `fetchAutoSaved re-emits the last stored guid`() =
    runTest {
      val repository = createRepository(localSave = true)
      val p = profile("auto")
      every { KeyValueStorage.getLastAutoSaveProfilesJson() } returns "guid-last"
      every { KeyValueStorage.decodeServerConfig("guid-last") } returns p

      val result = CompletableDeferred<ConnectionProfile?>()
      launch(UnconfinedTestDispatcher(testScheduler)) {
        result.complete(repository.autoSaved().first())
      }
      repository.fetchAutoSaved()

      assertEquals(p, withTimeout(2_000) { result.await() })
    }

  @Test
  fun `markAutoSavedSeen clears the stored guid and emits null profile`() =
    runTest {
      val repository = createRepository(localSave = true)
      every { KeyValueStorage.decodeServerConfig("") } returns null

      val result = CompletableDeferred<ConnectionProfile?>()
      launch(UnconfinedTestDispatcher(testScheduler)) {
        result.complete(repository.autoSaved().first())
      }
      repository.markAutoSavedSeen()

      verify(exactly = 1) { KeyValueStorage.setLastAutoSaveProfilesJson("") }
      assertNull(withTimeout(2_000) { result.await() })
    }

  private fun json(vararg profiles: ConnectionProfile): String = profiles.joinToString(separator = SEP) { JsonUtil.toJson(it) }

  private companion object {
    const val SEP = "########"
  }
}
