package com.example.funaggr.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class GoOutParserTest {

    private val body = """
        {
          "meta": {"nextScrollId": "abc", "donut": "d0"},
          "schedules": [
            {"id": 1, "type": "schedule", "url": "https://goout.net/schedule/x",
             "attributes": {"startAt": "2026-10-25T20:00:00+01:00", "endAt": "2026-10-25T23:00:00+01:00",
                            "hasTime": true, "pricing": "250–390", "currency": "czk", "ticketingState": "on_sale"},
             "locales": {"cs": {"siteUrl": "https://goout.net/cs/koncert/sz1/"}},
             "relationships": {"venue": {"id": 10, "type": "venue"}, "event": {"id": 100, "type": "event"}}},
            {"id": 2, "type": "schedule",
             "attributes": {"startAt": "2026-10-26T10:00:00+01:00", "hasTime": false, "pricing": "0",
                            "currency": "czk", "ticketingState": "free"},
             "locales": {"cs": {"siteUrl": "https://goout.net/cs/dilna/sz2/"}},
             "relationships": {"venue": {"id": 11, "type": "venue"}, "event": {"id": 101, "type": "event"}}},
            {"id": 3, "type": "schedule", "attributes": {},
             "relationships": {"venue": {"id": 10}, "event": {"id": 999}}}
          ],
          "included": {
            "events": [
              {"id": 100, "attributes": {"mainCategory": "concerts", "categories": ["concerts"]},
               "locales": {"cs": {"name": "Jazzový večer", "description": "**Skvělý** jazz [web](https://x)"}},
               "relationships": {"images": [{"id": 500, "type": "image"}]}},
              {"id": 101, "attributes": {"mainCategory": "play", "categories": ["play", "for_children"]},
               "locales": {"en": {"name": "Puppet workshop"}}}
            ],
            "venues": [
              {"id": 10, "attributes": {"latitude": 50.08, "longitude": 14.42, "city": "Praha 1"},
               "locales": {"cs": {"name": "Jazz klub"}}},
              {"id": 11, "attributes": {"latitude": 0.0, "longitude": 0.0, "city": "Internet"},
               "locales": {"cs": {"name": "Online"}}}
            ],
            "images": [{"id": 500, "attributes": {"url": "https://goout.net/i/1/500.jpg"}}]
          }
        }
    """.trimIndent()

    @Test
    fun `parses schedules joined with events venues and images`() {
        val page = GoOutParser.parse(body)

        assertEquals("abc", page.nextScrollId)
        assertEquals("d0", page.donut)
        assertEquals(2, page.items.size) // schedule 3 references an unknown event

        val concert = page.items[0]
        assertEquals("Jazzový večer", concert.title)
        assertEquals("Skvělý jazz web", concert.description)
        assertEquals(LocalDateTime.of(2026, 10, 25, 20, 0), concert.start)
        assertTrue(concert.hasTime)
        assertEquals("Jazz klub", concert.venueName)
        assertEquals(50.08, concert.latitude!!, 0.0001)
        assertEquals("250–390", concert.pricing)
        assertFalse(concert.isFree)
        assertEquals("https://goout.net/cs/koncert/sz1/", concert.url)
        assertEquals("https://goout.net/i/1/500.jpg", concert.imageUrl)
        assertEquals(listOf("concerts"), concert.categories)

        val workshop = page.items[1]
        assertEquals("Puppet workshop", workshop.title) // falls back to another locale
        assertTrue(workshop.isFree)
        assertNull(workshop.imageUrl)
        assertTrue("for_children" in workshop.categories)
    }

    @Test(expected = SourceException::class)
    fun `error response raises SourceException`() {
        GoOutParser.parse("""{"message":"Missing/invalid parameters"}""")
    }
}
