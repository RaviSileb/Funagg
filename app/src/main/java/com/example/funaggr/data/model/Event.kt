package com.example.funaggr.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single aggregated event.
 *
 * [dateStart]/[dateEnd] are ISO-8601 local values, either a date (`2026-10-25`) or a
 * date-time with minute precision (`2026-10-25T20:00`), so they sort lexicographically.
 */
@Entity(tableName = "events")
data class Event(
    @PrimaryKey val id: String,
    val title: String,
    val type: EventType,
    val description: String,
    val dateStart: String,
    val dateEnd: String? = null,
    val locationName: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    /** Distance from the location used for the fetch; null when the venue could not be located. */
    val distanceKm: Double? = null,
    /** True when the venue position is only a town/district centre, not the exact address. */
    val distanceApproximate: Boolean = false,
    /** Lowest known price in CZK, 0 for free events, null when unknown. */
    val priceCzk: Int? = null,
    val priceText: String? = null,
    val sourceUrl: String,
    val imageUrl: String? = null,
    val source: String,
    /** Number of collapsed dates of the same event at the same venue. */
    val occurrences: Int = 1,
    val cachedAt: Long = System.currentTimeMillis()
)
