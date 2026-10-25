package com.example.funaggr.data.remote

import com.example.funaggr.data.model.EventType
import org.junit.Assert.assertEquals
import org.junit.Test

class EventClassifierTest {

    @Test
    fun `classifies by Czech keywords in title`() {
        assertEquals(EventType.MARKET, EventClassifier.classify("Farmářské trhy na Kubáni 2026"))
        assertEquals(EventType.KIDS, EventClassifier.classify("Halloween pro děti s Honzou"))
        assertEquals(EventType.FESTIVAL, EventClassifier.classify("Prague Coffee Festival"))
        assertEquals(EventType.THEATER, EventClassifier.classify("Muzikálová jeskyně"))
        assertEquals(EventType.MUSIC, EventClassifier.classify("Brixáč XL – koncert akademického souboru"))
        assertEquals(EventType.EXHIBITION, EventClassifier.classify("Stáňa Franců – výstava obrazů"))
        assertEquals(EventType.SPORT, EventClassifier.classify("Velká Kunratická – běžecký závod"))
        assertEquals(EventType.EDUCATION, EventClassifier.classify("Architekti věčnosti – komentovaná vycházka"))
    }

    @Test
    fun `does not match keywords inside other words`() {
        // "hra" used to match "hrad"/"zahrada", "běh" must not match "během".
        assertEquals(EventType.OTHER, EventClassifier.classify("Zahrada u hradu", "Během dne otevřeno"))
    }

    @Test
    fun `falls back to description and source categories`() {
        assertEquals(EventType.MUSIC, EventClassifier.classify("Nikl & Slovák", "Večerní koncert v klubu"))
        assertEquals(EventType.KIDS, EventClassifier.classify("RozKrávka", categories = listOf("play", "for_children")))
        assertEquals(EventType.MUSIC, EventClassifier.classify("Temná sobota", categories = listOf("clubbing")))
        assertEquals(EventType.OTHER, EventClassifier.classify("Neznámá akce"))
    }
}
