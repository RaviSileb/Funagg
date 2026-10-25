package com.example.funaggr.data.remote

import android.util.Log
import com.example.funaggr.data.geo.GeoMath
import com.example.funaggr.data.geo.PlaceGeocoder
import com.example.funaggr.data.model.Event
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * kudyznudy.cz (CzechTourism) calendar, queried by GPS position and date range.
 * The site returns events ordered by proximity; venues are geocoded from the place name.
 */
@Singleton
class KudyZNudySource @Inject constructor(
    private val http: HttpFetcher,
    private val geocoder: PlaceGeocoder
) : EventSource {

    override val name = "Kudy z nudy"
    override val priority = 0

    override suspend fun fetchEvents(query: EventQuery): List<Event> = coroutineScope {
        val first = fetchPage(query, 1)
        val pageCount = first.totalPages.coerceAtMost(MAX_PAGES)
        val rest = (2..pageCount).map { page ->
            async { runCatching { fetchPage(query, page) }.getOrNull() }
        }.awaitAll().filterNotNull()

        val pages = listOf(first) + rest
        Log.d(TAG, "Pages ${pages.size}/${first.totalPages}, strategy ${first.strategy}")
        val items = pages.flatMap { it.items }
            .filter { item -> (item.endDate ?: item.startDate)?.isBefore(query.from) != true }

        val places = items.map { it.place }.filter { it.isNotBlank() }.distinct()
        val permits = Semaphore(GEOCODE_PARALLELISM)
        val coordinates = places.map { place ->
            async { place to permits.withPermit { geocoder.locate(place) } }
        }.awaitAll().toMap()

        items
            .groupBy { it.url to it.place }
            .map { (_, occurrences) ->
                val item = occurrences.minBy { it.startDate ?: query.from }
                val point = coordinates[item.place]
                val end = occurrences.mapNotNull { it.endDate ?: it.startDate }.maxOrNull()
                Event(
                    id = "kzn:${item.url}|${item.place}",
                    title = item.title,
                    type = EventClassifier.classify(item.title, item.description),
                    description = item.description,
                    dateStart = (item.startDate ?: query.from).toString(),
                    dateEnd = end?.takeIf { it != item.startDate }?.toString(),
                    locationName = item.place.ifBlank { "Neurčeno" },
                    latitude = point?.latitude,
                    longitude = point?.longitude,
                    distanceKm = point?.let {
                        GeoMath.distanceKm(query.location.latitude, query.location.longitude, it.latitude, it.longitude)
                    },
                    distanceApproximate = true,
                    priceCzk = TextParsing.parsePriceCzk(item.description)?.takeIf { it == 0 },
                    sourceUrl = item.url,
                    imageUrl = item.imageUrl,
                    source = name,
                    occurrences = occurrences.size
                )
            }
    }

    private suspend fun fetchPage(query: EventQuery, page: Int): KudyZNudyParser.Page {
        val url = buildString {
            append(BASE_URL).append("/kalendar-akci?datum=")
            append(query.from).append(',').append(query.until)
            append("&lat=").append(String.format(Locale.US, "%.6f", query.location.latitude))
            append("&lon=").append(String.format(Locale.US, "%.6f", query.location.longitude))
            if (page > 1) append("&stranka=").append(page)
        }
        val html = http.get(url, accept = "text/html")
        return KudyZNudyParser.parse(html, url).also {
            Log.d(TAG, "Page $page: ${it.items.size} items (${it.strategy})")
        }
    }

    private companion object {
        const val TAG = "KudyZNudySource"
        const val BASE_URL = "https://www.kudyznudy.cz"
        const val MAX_PAGES = 5
        const val GEOCODE_PARALLELISM = 4
    }
}
