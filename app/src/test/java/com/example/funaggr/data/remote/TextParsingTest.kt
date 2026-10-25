package com.example.funaggr.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TextParsingTest {

    @Test
    fun `parses prices`() {
        assertEquals(0, TextParsing.parsePriceCzk("Vstup zdarma"))
        assertEquals(449, TextParsing.parsePriceCzk("449–1299"))
        assertEquals(1290, TextParsing.parsePriceCzk("od 1 290 Kč"))
        assertNull(TextParsing.parsePriceCzk(null))
        assertNull(TextParsing.parsePriceCzk("cena neuvedena"))
    }

    @Test
    fun `strips markdown and shortens text`() {
        assertEquals("Tučně a odkaz", TextParsing.plainText("**Tučně** a [odkaz](https://x.cz)"))
        val long = "slovo ".repeat(100)
        val short = TextParsing.plainText(long, maxLength = 20)
        assertEquals("slovo slovo slovo…", short)
    }
}
