package com.example.funaggr.data.remote

import android.util.Log
import com.example.funaggr.data.geo.CzechPlaces
import com.example.funaggr.data.geo.GeoMath
import com.example.funaggr.data.model.Event
import java.net.URLEncoder
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * GoOut.net public entities API. Events are requested for the nearest supported city;
 * `donuts=true` makes the API continue with rings of surrounding towns on later pages.
 * Venues carry exact coordinates, so distances are precise.
 */
@Singleton
class GoOutSource @Inject constructor(
    private val http: HttpFetcher
) : EventSource {

    override val name = "GoOut"
    override val priority = 1

    override suspend fun fetchEvents(query: EventQuery): List<Event> {
        val location = query.location
        val city = CzechPlaces.nearestGoOutCity(location.latitude, location.longitude)
        val items = mutableListOf<GoOutParser.Item>()
        var scrollId: String? = null

        for (pageIndex in 0 until MAX_PAGES) {
            val url = buildString {
                append(API_URL)
                append("?languages%5B%5D=cs&source=goout.net&include=events,venues,images")
                append("&limit=").append(PAGE_SIZE)
                append("&donuts=true&cityIds%5B%5D=").append(city.goOutCityId)
                scrollId?.let { append("&scrollId=").append(URLEncoder.encode(it, "UTF-8")) }
            }
            val page = GoOutParser.parse(http.get(url, accept = "application/json"))
            items += page.items
            Log.d(TAG, "Page $pageIndex (${city.name}, donut ${page.donut}): ${page.items.size} items")

            val nearby = page.items.count { item -> distanceTo(item, query)?.let { it <= query.radiusKm } == true }
            val beyondRange = page.items.isNotEmpty() &&
                page.items.all { it.start?.toLocalDate()?.isAfter(query.until) == true }
            val leftRadius = page.donut != null && page.donut != "d0" && nearby == 0
            scrollId = page.nextScrollId
            if (scrollId == null || page.items.isEmpty() || beyondRange || leftRadius) break
        }

        return items
            .filter { item ->
                val day = item.start?.toLocalDate() ?: return@filter false
                val lastDay = item.end?.toLocalDate() ?: day
                !lastDay.isBefore(query.from) && !day.isAfter(query.until)
            }
            .mapNotNull { item -> distanceTo(item, query)?.takeIf { it <= query.radiusKm }?.let { item to it } }
            .groupBy { (item, _) -> item.eventId to item.venueId }
            .map { (_, group) ->
                val (item, distance) = group.minBy { it.first.start!! }
                val lastStart = group.maxOf { it.first.start!! }.toLocalDate()
                val isCzk = item.currency.equals("czk", ignoreCase = true)
                Event(
                    id = "goout:${item.eventId}|${item.venueId}",
                    title = item.title,
                    type = EventClassifier.classify(item.title, item.description, item.categories),
                    description = item.description,
                    dateStart = formatStart(item),
                    dateEnd = lastStart.takeIf { it != item.start!!.toLocalDate() }?.toString(),
                    locationName = listOf(item.venueName, item.venueCity).filter { it.isNotBlank() }
                        .distinct().joinToString(", ").ifBlank { "Neurčeno" },
                    latitude = item.latitude,
                    longitude = item.longitude,
                    distanceKm = distance,
                    priceCzk = if (item.isFree) 0 else if (isCzk) TextParsing.parsePriceCzk(item.pricing) else null,
                    priceText = item.pricing?.takeIf { !item.isFree && it.isNotBlank() }
                        ?.let { if (isCzk) "$it Kč" else if (item.currency.equals("eur", true)) "$it €" else it },
                    sourceUrl = item.url,
                    imageUrl = item.imageUrl,
                    source = name,
                    occurrences = group.size
                )
            }
    }

    private fun distanceTo(item: GoOutParser.Item, query: EventQuery): Double? {
        if (!GeoMath.isValid(item.latitude, item.longitude)) return null
        return GeoMath.distanceKm(
            query.location.latitude, query.location.longitude, item.latitude!!, item.longitude!!
        )
    }

    private fun formatStart(item: GoOutParser.Item): String {
        val start = item.start!!
        return if (item.hasTime) start.format(DATE_TIME) else start.toLocalDate().toString()
    }

    private companion object {
        const val TAG = "GoOutSource"
        const val API_URL = "https://goout.net/services/entities/v1/schedules"
        const val PAGE_SIZE = 100
        const val MAX_PAGES = 3
        val DATE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
    }
}
