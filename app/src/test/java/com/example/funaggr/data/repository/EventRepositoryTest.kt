package com.example.funaggr.data.repository

import com.example.funaggr.data.local.CacheMetadata
import com.example.funaggr.data.local.EventDao
import com.example.funaggr.data.local.FetchMeta
import com.example.funaggr.data.model.Event
import com.example.funaggr.data.model.EventType
import com.example.funaggr.data.model.LocationData
import com.example.funaggr.data.remote.EventQuery
import com.example.funaggr.data.remote.EventSource
import com.example.funaggr.data.remote.SourceException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class EventRepositoryTest {

    private val prague = LocationData(50.0335, 14.5120, "Praha 11")
    private val now = LocalDate.of(2026, 10, 25).atTime(12, 0).atZone(ZoneId.of("Europe/Prague"))
        .toInstant().toEpochMilli()

    private class FakeDao : EventDao() {
        val events = MutableStateFlow<List<Event>>(emptyList())
        var replaceCalls = 0
        override fun observeAll(): Flow<List<Event>> = events
        override suspend fun getAll(): List<Event> = events.value
        override suspend fun count(): Int = events.value.size
        override suspend fun insertAll(events: List<Event>) {
            this.events.value = this.events.value + events
        }
        override suspend fun deleteAll() {
            events.value = emptyList()
        }
        override suspend fun replaceAll(events: List<Event>) {
            replaceCalls++
            super.replaceAll(events)
        }
    }

    private class FakeMeta(var meta: FetchMeta? = null) : CacheMetadata {
        override fun read() = meta
        override fun write(meta: FetchMeta) {
            this.meta = meta
        }
    }

    private class FakeSource(
        override val name: String,
        override val priority: Int,
        var result: () -> List<Event>
    ) : EventSource {
        var calls = 0
        override suspend fun fetchEvents(query: EventQuery): List<Event> {
            calls++
            return result()
        }
    }

    private fun event(id: String, source: String, distance: Double = 2.0) = Event(
        id = id, title = "Akce $id", type = EventType.MUSIC, description = "", dateStart = "2026-10-26",
        locationName = "Praha", distanceKm = distance, sourceUrl = "https://example.com/$id", source = source
    )

    private fun repository(dao: FakeDao, meta: FakeMeta, vararg sources: EventSource) =
        EventRepository(dao, sources.toSet(), meta).also { it.clock = { now } }

    @Test
    fun `combines all sources when they succeed`() = runTest {
        val dao = FakeDao()
        val kzn = FakeSource("Kudy z nudy", 0) { listOf(event("a", "Kudy z nudy")) }
        val goout = FakeSource("GoOut", 1) { listOf(event("b", "GoOut", 1.0)) }

        val outcome = repository(dao, FakeMeta(), kzn, goout).refresh(prague, force = true)

        outcome as RefreshOutcome.Fetched
        assertEquals(2, outcome.eventCount)
        assertTrue(outcome.reports.all { it.isSuccess })
        assertEquals(listOf("b", "a"), dao.events.value.map { it.id })
    }

    @Test
    fun `one failing source does not break the others`() = runTest {
        val dao = FakeDao()
        val kzn = FakeSource("Kudy z nudy", 0) { throw SourceException("HTTP 503") }
        val goout = FakeSource("GoOut", 1) { listOf(event("b", "GoOut")) }

        val outcome = repository(dao, FakeMeta(), kzn, goout).refresh(prague, force = true)

        outcome as RefreshOutcome.Fetched
        assertEquals(listOf("b"), dao.events.value.map { it.id })
        val failed = outcome.reports.single { !it.isSuccess }
        assertEquals("Kudy z nudy", failed.name)
        assertEquals("HTTP 503", failed.error)
    }

    @Test
    fun `failed source falls back to its previously cached events`() = runTest {
        val dao = FakeDao().apply { events.value = listOf(event("old-kzn", "Kudy z nudy"), event("old-go", "GoOut")) }
        val meta = FakeMeta(FetchMeta(now - 2 * 60 * 60 * 1000, prague.latitude, prague.longitude, 25))
        val kzn = FakeSource("Kudy z nudy", 0) { throw SourceException("timeout") }
        val goout = FakeSource("GoOut", 1) { listOf(event("new-go", "GoOut")) }

        val outcome = repository(dao, meta, kzn, goout).refresh(prague, force = true)

        outcome as RefreshOutcome.Fetched
        assertEquals(setOf("old-kzn", "new-go"), dao.events.value.map { it.id }.toSet())
        val kznReport = outcome.reports.single { it.name == "Kudy z nudy" }
        assertTrue(kznReport.fromCache)
        assertEquals(1, kznReport.eventCount)
    }

    @Test
    fun `cache is kept untouched when every source fails`() = runTest {
        val cached = listOf(event("old", "Kudy z nudy"))
        val dao = FakeDao().apply { events.value = cached }
        val meta = FakeMeta(FetchMeta(now - 60 * 60 * 1000, prague.latitude, prague.longitude, 25))
        val failing = { throw SourceException("offline") }

        val outcome = repository(dao, meta, FakeSource("Kudy z nudy", 0, failing), FakeSource("GoOut", 1, failing))
            .refresh(prague, force = true)

        outcome as RefreshOutcome.Failed
        assertTrue(outcome.hasCache)
        assertEquals(cached, dao.events.value)
        assertEquals(0, dao.replaceCalls)
    }

    @Test
    fun `empty results from a failing source never wipe the cache`() = runTest {
        // Regression: the old scraper returned "success" with 0 events and cleared the cache.
        val dao = FakeDao().apply { events.value = listOf(event("old", "GoOut")) }
        val meta = FakeMeta(FetchMeta(now - 60 * 60 * 1000, prague.latitude, prague.longitude, 25))
        val kzn = FakeSource("Kudy z nudy", 0) { emptyList() }
        val goout = FakeSource("GoOut", 1) { throw SourceException("HTTP 500") }

        val outcome = repository(dao, meta, kzn, goout).refresh(prague, force = true)

        outcome as RefreshOutcome.Failed
        assertEquals(listOf("old"), dao.events.value.map { it.id })
    }

    @Test
    fun `fresh cache for the same place is reused without network`() = runTest {
        val dao = FakeDao().apply { events.value = listOf(event("cached", "GoOut")) }
        val meta = FakeMeta(FetchMeta(now - 5 * 60 * 1000, prague.latitude, prague.longitude, 25))
        val source = FakeSource("GoOut", 1) { listOf(event("new", "GoOut")) }

        val outcome = repository(dao, meta, source).refresh(prague)

        assertTrue(outcome is RefreshOutcome.FromCache)
        assertEquals(0, source.calls)
    }

    @Test
    fun `cache from a distant place is refreshed`() = runTest {
        val dao = FakeDao().apply { events.value = listOf(event("cached", "GoOut")) }
        val meta = FakeMeta(FetchMeta(now - 5 * 60 * 1000, 49.1951, 16.6068, 25)) // Brno
        val source = FakeSource("GoOut", 1) { listOf(event("new", "GoOut")) }

        val outcome = repository(dao, meta, source).refresh(prague)

        assertTrue(outcome is RefreshOutcome.Fetched)
        assertEquals(1, source.calls)
        assertFalse(dao.events.value.any { it.id == "cached" })
    }
}
