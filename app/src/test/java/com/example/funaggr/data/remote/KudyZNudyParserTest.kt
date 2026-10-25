package com.example.funaggr.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class KudyZNudyParserTest {

    private val base = "https://www.kudyznudy.cz/kalendar-akci?datum=2026-10-25"

    private fun microdataItem(slug: String, name: String, start: String, end: String, place: String) = """
        <div class="item one-fourth" itemscope itemtype="http://schema.org/Event">
            <div class="circle-date"><span>24. 10.</span></div>
            <meta itemprop="startDate" content="$start" />
            <meta itemprop="endDate" content="$end" />
            <meta itemprop="description" content="Popis akce $name, vstup zdarma." />
            <div class="image"><a href="/akce/$slug" title="$name">
                <img src="https://cdn.example.com/$slug.webp" alt="$name" itemprop="image" /></a></div>
            <div class="text">
                <h3><a href="/akce/$slug" title="$name"><span itemprop="name">$name</span></a></h3>
                <div class="location" itemprop="location" itemscope itemtype="http://schema.org/Place">
                    <span itemprop="name">$place</span></div>
            </div>
        </div>
    """

    @Test
    fun `parses schema org microdata items and page count`() {
        val html = """
            <html><body>
            ${microdataItem("farmarske-trhy", "Farmářské trhy", "2026-10-27", "2026-10-27", "Praha 10, Praha")}
            ${microdataItem("koncert", "Podzimní koncert", "2026-10-24", "2026-10-31", "Brno-střed, Brno")}
            <div class="paging"><a href="/kalendar-akci?datum=2026-10-25&stranka=2#kalendar">2</a>
            <a href="/kalendar-akci?datum=2026-10-25&stranka=7">7</a></div>
            </body></html>
        """.trimIndent()

        val page = KudyZNudyParser.parse(html, base)

        assertEquals("microdata", page.strategy)
        assertEquals(7, page.totalPages)
        assertEquals(2, page.items.size)
        val first = page.items[0]
        assertEquals("Farmářské trhy", first.title)
        assertEquals("https://www.kudyznudy.cz/akce/farmarske-trhy", first.url)
        assertEquals(LocalDate.of(2026, 10, 27), first.startDate)
        assertEquals("Praha 10, Praha", first.place)
        assertEquals("https://cdn.example.com/farmarske-trhy.webp", first.imageUrl)
        assertTrue(first.description.contains("zdarma"))
        assertEquals(LocalDate.of(2026, 10, 31), page.items[1].endDate)
    }

    @Test
    fun `falls back to JSON-LD when microdata is missing`() {
        val html = """
            <html><head><script type="application/ld+json">
            [{"@context":"https://schema.org","@type":"Event","name":"Vánoční jarmark",
              "url":"/akce/vanocni-jarmark","startDate":"2026-12-01T10:00:00",
              "location":{"@type":"Place","name":"Náměstí","address":{"addressLocality":"Kolín"}}}]
            </script></head><body></body></html>
        """.trimIndent()

        val page = KudyZNudyParser.parse(html, base)

        assertEquals("json-ld", page.strategy)
        assertEquals(1, page.items.size)
        assertEquals("https://www.kudyznudy.cz/akce/vanocni-jarmark", page.items[0].url)
        assertEquals("Náměstí, Kolín", page.items[0].place)
        assertEquals(LocalDate.of(2026, 12, 1), page.items[0].startDate)
    }

    @Test
    fun `falls back to plain event links as last resort`() {
        val html = """
            <html><body>
            <nav><a href="/akce/menu-odkaz">Menu odkaz</a></nav>
            <div><a href="/akce/den-otevrenych-dveri" title="Den otevřených dveří">x</a></div>
            </body></html>
        """.trimIndent()

        val page = KudyZNudyParser.parse(html, base)

        assertEquals("links", page.strategy)
        assertEquals(listOf("Den otevřených dveří"), page.items.map { it.title })
    }

    @Test
    fun `empty page yields no items and single page`() {
        val page = KudyZNudyParser.parse("<html><body><p>Nic</p></body></html>", base)
        assertTrue(page.items.isEmpty())
        assertEquals(1, page.totalPages)
    }
}
