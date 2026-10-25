package com.example.funaggr.data.repository

import android.util.Log
import com.example.funaggr.data.geo.GeoMath
import com.example.funaggr.data.local.CacheMetadata
import com.example.funaggr.data.local.EventDao
import com.example.funaggr.data.local.FetchMeta
import com.example.funaggr.data.model.Event
import com.example.funaggr.data.model.LocationData
import com.example.funaggr.data.remote.EventQuery
import com.example.funaggr.data.remote.EventSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** Result of one source in a refresh. */
data class SourceReport(
    val name: String,
    val eventCount: Int,
    val error: String? = null,
    /** True when the source failed and its previously cached events were kept instead. */
    val fromCache: Boolean = false
) {
    val isSuccess: Boolean get() = error == null
}

sealed interface RefreshOutcome {
    /** The cache was fresh for this location; nothing was downloaded. */
    data class FromCache(val fetchedAt: Long) : RefreshOutcome

    /** New data was stored. Some sources may have failed (see [reports]). */
    data class Fetched(val fetchedAt: Long, val reports: List<SourceReport>, val eventCount: Int) : RefreshOutcome

    /** Nothing usable was downloaded; the previous cache (if any) is left untouched. */
    data class Failed(val reports: List<SourceReport>, val hasCache: Boolean, val cachedAt: Long?) : RefreshOutcome
}

@Singleton
class EventRepository @Inject constructor(
    private val eventDao: EventDao,
    private val sources: Set<@JvmSuppressWildcards EventSource>,
    private val cacheMetadata: CacheMetadata
) {
    /** Overridable for tests. */
    internal var clock: () -> Long = System::currentTimeMillis

    /** Sources parse large HTML/JSON documents, so they run off the main thread. */
    internal var dispatcher: CoroutineDispatcher = Dispatchers.Default

    fun observeEvents(): Flow<List<Event>> = eventDao.observeAll()

    suspend fun refresh(
        location: LocationData,
        radiusKm: Int = DEFAULT_RADIUS_KM,
        force: Boolean = false
    ): RefreshOutcome {
        val now = clock()
        val meta = cacheMetadata.read()
        val cacheMatchesLocation = meta != null && meta.radiusKm == radiusKm &&
            GeoMath.distanceKm(meta.latitude, meta.longitude, location.latitude, location.longitude) <=
            CACHE_LOCATION_TOLERANCE_KM

        if (!force && meta != null && cacheMatchesLocation && now - meta.fetchedAt < CACHE_TTL_MS && eventDao.count() > 0) {
            Log.d(TAG, "Using cached events from ${meta.fetchedAt}")
            return RefreshOutcome.FromCache(meta.fetchedAt)
        }

        val today = Instant.ofEpochMilli(now).atZone(ZONE).toLocalDate()
        val query = EventQuery(location, radiusKm, today, DAYS_AHEAD)
        val results = withContext(dispatcher) { fetchAll(query) }

        val previous = if (cacheMatchesLocation && results.any { it.second.isFailure }) eventDao.getAll() else emptyList()

        val reports = mutableListOf<SourceReport>()
        val collected = mutableListOf<Event>()
        for ((source, result) in results) {
            result.fold(
                onSuccess = { events ->
                    collected += events
                    reports += SourceReport(source.name, events.size)
                },
                onFailure = { error ->
                    Log.w(TAG, "${source.name} failed: ${error.message}", error)
                    val fallback = previous.filter { it.source == source.name }
                    collected += fallback
                    reports += SourceReport(
                        name = source.name,
                        eventCount = fallback.size,
                        error = error.message ?: error::class.java.simpleName,
                        fromCache = fallback.isNotEmpty()
                    )
                }
            )
        }

        val merged = withContext(dispatcher) { EventMerger.merge(collected, today, radiusKm) }
        val freshCount = reports.filter { it.isSuccess }.sumOf { it.eventCount }
        val allSucceeded = reports.all { it.isSuccess }
        Log.d(TAG, "Refresh: ${reports.joinToString { "${it.name}=${it.eventCount}${it.error?.let { e -> " ($e)" } ?: ""}" }} -> ${merged.size}")

        // Replace the cache only with fresh data, or when every source genuinely reported nothing.
        if (freshCount > 0 || allSucceeded) {
            eventDao.replaceAll(merged.map { it.copy(cachedAt = now) })
            cacheMetadata.write(FetchMeta(now, location.latitude, location.longitude, radiusKm))
            return RefreshOutcome.Fetched(now, reports, merged.size)
        }
        return RefreshOutcome.Failed(reports, hasCache = eventDao.count() > 0, cachedAt = meta?.fetchedAt)
    }

    private suspend fun fetchAll(query: EventQuery): List<Pair<EventSource, Result<List<Event>>>> =
        coroutineScope {
            sources.sortedBy { it.priority }.map { source ->
                async { source to fetchSafely(source, query) }
            }.awaitAll()
        }

    private suspend fun fetchSafely(source: EventSource, query: EventQuery): Result<List<Event>> =
        try {
            Result.success(withTimeout(SOURCE_TIMEOUT_MS) { source.fetchEvents(query) })
        } catch (e: TimeoutCancellationException) {
            Result.failure(Exception("Časový limit vypršel", e))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    companion object {
        private const val TAG = "EventRepository"
        const val DEFAULT_RADIUS_KM = 25
        const val DAYS_AHEAD = 14
        const val CACHE_TTL_MS = 30 * 60 * 1000L
        const val CACHE_LOCATION_TOLERANCE_KM = 3.0
        const val SOURCE_TIMEOUT_MS = 30_000L
        private val ZONE: ZoneId = ZoneId.of("Europe/Prague")
    }
}
