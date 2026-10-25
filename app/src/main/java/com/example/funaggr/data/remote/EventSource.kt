package com.example.funaggr.data.remote

import com.example.funaggr.data.model.Event
import com.example.funaggr.data.model.LocationData
import java.time.LocalDate

data class EventQuery(
    val location: LocationData,
    val radiusKm: Int,
    val from: LocalDate,
    val days: Int
) {
    val until: LocalDate get() = from.plusDays(days.toLong() - 1)
}

/**
 * One independent provider of events. Implementations throw on failure; the repository
 * runs all sources in parallel and falls back per source when one of them fails.
 */
interface EventSource {
    val name: String

    /** Lower value wins when the same event comes from several sources. */
    val priority: Int

    suspend fun fetchEvents(query: EventQuery): List<Event>
}

class SourceException(message: String, cause: Throwable? = null) : Exception(message, cause)
