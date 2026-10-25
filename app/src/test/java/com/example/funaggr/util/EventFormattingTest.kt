package com.example.funaggr.util

import com.example.funaggr.data.model.Event
import com.example.funaggr.data.model.EventType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class EventFormattingTest {

    private val today = LocalDate.of(2026, 10, 25)

    private fun event(start: String, end: String? = null, occurrences: Int = 1) = Event(
        id = "x", title = "x", type = EventType.OTHER, description = "", dateStart = start, dateEnd = end,
        locationName = "Praha", sourceUrl = "https://example.com", source = "test", occurrences = occurrences
    )

    @Test
    fun `formats dates relative to today`() {
        assertEquals("dnes 20:00", EventFormatting.date(event("2026-10-25T20:00"), today))
        assertEquals("zítra", EventFormatting.date(event("2026-10-26"), today))
        assertEquals("probíhá do 31. 10.", EventFormatting.date(event("2026-09-24", "2026-10-31"), today))
        assertEquals("zítra + další termíny (2)", EventFormatting.date(event("2026-10-26", "2026-11-07", 3), today))
    }

    @Test
    fun `formats distances`() {
        assertEquals(null, EventFormatting.distance(null))
        assertEquals("350 m", EventFormatting.distance(0.37))
        assertEquals("4,2 km", EventFormatting.distance(4.21))
        assertEquals("≈ 1 km", EventFormatting.distance(0.15, approximate = true))
        assertEquals("≈ 5 km", EventFormatting.distance(4.9, approximate = true))
    }
}
