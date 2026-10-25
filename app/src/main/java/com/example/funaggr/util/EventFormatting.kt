package com.example.funaggr.util

import com.example.funaggr.data.model.Event
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Czech presentation of event dates, distances and prices. */
object EventFormatting {
    private val czech = Locale.forLanguageTag("cs-CZ")
    private val dayFormat = DateTimeFormatter.ofPattern("EEE d. M.", czech)
    private val shortDayFormat = DateTimeFormatter.ofPattern("d. M.", czech)
    private val timeFormat = DateTimeFormatter.ofPattern("H:mm", czech)

    fun date(event: Event, today: LocalDate = LocalDate.now()): String {
        val startDate = parseDate(event.dateStart) ?: return event.dateStart
        val endDate = event.dateEnd?.let(::parseDate)
        val time = if (event.dateStart.length > 10) {
            runCatching { LocalDateTime.parse(event.dateStart).format(timeFormat) }.getOrNull()
        } else null

        return when {
            event.occurrences > 1 -> buildString {
                append(dayLabel(startDate, today))
                time?.let { append(' ').append(it) }
                append(" + další termíny (").append(event.occurrences - 1).append(')')
            }
            endDate != null && endDate.isAfter(startDate) && startDate.isBefore(today) ->
                "probíhá do ${endDate.format(shortDayFormat)}"
            endDate != null && endDate.isAfter(startDate) ->
                "${dayLabel(startDate, today)} – ${endDate.format(shortDayFormat)}"
            else -> listOfNotNull(dayLabel(startDate, today), time).joinToString(" ")
        }
    }

    fun distance(km: Double?, approximate: Boolean = false): String? = when {
        km == null -> null
        approximate -> "≈ ${maxOf(1, Math.round(km).toInt())} km"
        km < 1.0 -> "${maxOf(50, (km * 1000).toInt() / 50 * 50)} m"
        else -> String.format(czech, "%.1f km", km)
    }

    fun price(event: Event): String? = when {
        event.priceCzk == 0 -> "Zdarma"
        event.priceText != null -> event.priceText
        event.priceCzk != null -> "${event.priceCzk} Kč"
        else -> null
    }

    private fun dayLabel(date: LocalDate, today: LocalDate): String = when (date) {
        today -> "dnes"
        today.plusDays(1) -> "zítra"
        else -> date.format(dayFormat)
    }

    private fun parseDate(value: String): LocalDate? =
        runCatching { LocalDate.parse(value.take(10)) }.getOrNull()
}
