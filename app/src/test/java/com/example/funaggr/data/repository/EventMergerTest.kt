package com.example.funaggr.data.repository

import com.example.funaggr.data.model.Event
import com.example.funaggr.data.model.EventType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class EventMergerTest {

    private val today = LocalDate.of(2026, 10, 25)

    private fun event(
        id: String,
        title: String = id,
        start: String = "2026-10-26",
        end: String? = null,
        distance: Double? = 1.0,
        source: String = "Kudy z nudy",
        image: String? = null
    ) = Event(
        id = id, title = title, type = EventType.OTHER, description = "", dateStart = start, dateEnd = end,
        locationName = "Praha", distanceKm = distance, sourceUrl = "https://example.com/$id",
        source = source, imageUrl = image
    )

    @Test
    fun `drops past events but keeps ongoing multi-day ones`() {
        val merged = EventMerger.merge(
            listOf(
                event("past", start = "2026-10-20"),
                event("ongoing", start = "2026-09-24", end = "2026-10-31"),
                event("future", start = "2026-10-30")
            ),
            today, radiusKm = 25
        )
        assertEquals(setOf("ongoing", "future"), merged.map { it.id }.toSet())
    }

    @Test
    fun `filters by radius and keeps events with unknown distance at the end`() {
        val merged = EventMerger.merge(
            listOf(
                event("far", distance = 80.0),
                event("unknown", distance = null),
                event("near", distance = 0.5),
                event("mid", distance = 7.0)
            ),
            today, radiusKm = 25
        )
        assertEquals(listOf("near", "mid", "unknown"), merged.map { it.id })
    }

    @Test
    fun `removes cross-source duplicates and keeps the richer record`() {
        val merged = EventMerger.merge(
            listOf(
                event("kzn", title = "Prague Coffee Festival 2026", start = "2026-10-26", distance = null),
                event(
                    "goout", title = "Prague Coffee Festival", start = "2026-10-26T10:00",
                    distance = 4.2, source = "GoOut", image = "https://img"
                )
            ),
            today, radiusKm = 25
        )
        assertEquals(listOf("goout"), merged.map { it.id })
    }
}
