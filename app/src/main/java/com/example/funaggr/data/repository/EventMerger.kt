package com.example.funaggr.data.repository

import com.example.funaggr.data.model.Event
import java.text.Normalizer
import java.time.LocalDate
import java.util.Locale

/** Combines events from all sources: drops past/out-of-range ones, removes duplicates, sorts by distance. */
object EventMerger {

    private val diacritics = Regex("\\p{M}+")
    private val years = Regex("\\b(19|20)\\d{2}\\b")
    private val nonAlphanumeric = Regex("[^a-z0-9]+")

    fun merge(events: List<Event>, today: LocalDate, radiusKm: Int): List<Event> =
        events
            .filter { lastDay(it)?.isBefore(today) != true }
            .filter { it.distanceKm == null || it.distanceKm <= radiusKm }
            .groupBy { dedupeKey(it) }
            .map { (_, duplicates) -> duplicates.maxWith(compareBy<Event> { richness(it) }.thenBy { -it.priorityHint() }) }
            .sortedWith(compareBy<Event, Double?>(nullsLast()) { it.distanceKm }.thenBy { it.dateStart })

    fun dedupeKey(event: Event): String {
        val title = Normalizer.normalize(event.title.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(diacritics, "")
            .replace(years, " ")
            .replace(nonAlphanumeric, "")
        return "$title|${event.dateStart.take(10)}"
    }

    private fun lastDay(event: Event): LocalDate? =
        runCatching { LocalDate.parse((event.dateEnd ?: event.dateStart).take(10)) }.getOrNull()

    /** Prefers the duplicate carrying the most useful details. */
    private fun richness(event: Event): Int =
        (if (event.distanceKm == null) 0 else if (event.distanceApproximate) 2 else 4) +
            (if (event.dateStart.length > 10) 2 else 0) +
            (if (event.priceCzk != null || event.priceText != null) 1 else 0) +
            (if (event.imageUrl != null) 1 else 0)

    private fun Event.priorityHint(): Int = if (source == "Kudy z nudy") 0 else 1
}
