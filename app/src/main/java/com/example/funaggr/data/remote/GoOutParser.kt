package com.example.funaggr.data.remote

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import java.time.LocalDateTime
import java.time.OffsetDateTime

/**
 * Parses responses of the GoOut entities API (`/services/entities/v1/schedules`).
 * Uses the untyped JSON tree so that new or missing fields never break parsing.
 */
object GoOutParser {

    data class Item(
        val scheduleId: String,
        val eventId: String,
        val venueId: String,
        val title: String,
        val description: String,
        val start: LocalDateTime?,
        val end: LocalDateTime?,
        val hasTime: Boolean,
        val venueName: String,
        val venueCity: String,
        val latitude: Double?,
        val longitude: Double?,
        val pricing: String?,
        val currency: String?,
        val isFree: Boolean,
        val url: String,
        val imageUrl: String?,
        val categories: List<String>
    )

    data class Page(val items: List<Item>, val nextScrollId: String?, val donut: String?)

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun parse(body: String, language: String = "cs"): Page {
        val root = json.parseToJsonElement(body) as? JsonObject
            ?: throw SourceException("Neočekávaná odpověď GoOut API")
        val schedules = root["schedules"] as? JsonArray
            ?: throw SourceException((root["message"] as? JsonPrimitive)?.contentOrNull ?: "GoOut API bez dat")
        val included = root["included"] as? JsonObject
        val events = included.index("events")
        val venues = included.index("venues")
        val images = included.index("images")
        val meta = root["meta"] as? JsonObject

        val items = schedules.mapNotNull { element ->
            val schedule = element as? JsonObject ?: return@mapNotNull null
            val attributes = schedule.obj("attributes")
            val relationships = schedule.obj("relationships")
            val eventId = relationships?.obj("event")?.str("id") ?: return@mapNotNull null
            val venueId = relationships.obj("venue")?.str("id").orEmpty()
            val event = events[eventId]
            val venue = venues[venueId]
            val eventLocale = event.locale(language)
            val venueAttributes = venue?.obj("attributes")
            val title = eventLocale?.str("name")?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            val url = schedule.locale(language)?.str("siteUrl") ?: schedule.str("url") ?: return@mapNotNull null
            val imageId = (event?.obj("relationships")?.get("images") as? JsonArray)
                ?.firstOrNull()?.let { (it as? JsonObject)?.str("id") }
            val eventAttributes = event?.obj("attributes")
            val categories = buildList {
                eventAttributes?.str("mainCategory")?.let(::add)
                (eventAttributes?.get("categories") as? JsonArray)?.forEach { c ->
                    (c as? JsonPrimitive)?.contentOrNull?.let(::add)
                }
            }.distinct()

            Item(
                scheduleId = schedule.str("id").orEmpty(),
                eventId = eventId,
                venueId = venueId,
                title = title,
                description = TextParsing.plainText(eventLocale.str("description")),
                start = parseDateTime(attributes?.str("startAt")),
                end = parseDateTime(attributes?.str("endAt")),
                hasTime = attributes?.bool("hasTime") ?: false,
                venueName = venue.locale(language)?.str("name").orEmpty(),
                venueCity = venueAttributes?.str("city").orEmpty(),
                latitude = venueAttributes?.double("latitude"),
                longitude = venueAttributes?.double("longitude"),
                pricing = attributes?.str("pricing"),
                currency = attributes?.str("currency"),
                isFree = attributes?.str("ticketingState") == "free" || attributes?.str("pricing") == "0",
                url = url,
                imageUrl = imageId?.let { images[it]?.obj("attributes")?.str("url") },
                categories = categories
            )
        }
        return Page(items, meta?.str("nextScrollId"), meta?.str("donut"))
    }

    private fun parseDateTime(value: String?): LocalDateTime? =
        value?.let { runCatching { OffsetDateTime.parse(it).toLocalDateTime() }.getOrNull() }

    private fun JsonObject?.index(key: String): Map<String, JsonObject> =
        (this?.get(key) as? JsonArray)
            ?.mapNotNull { it as? JsonObject }
            ?.mapNotNull { obj -> obj.str("id")?.let { it to obj } }
            ?.toMap()
            ?: emptyMap()

    private fun JsonObject?.locale(language: String): JsonObject? {
        val locales = this?.obj("locales") ?: return null
        return locales.obj(language) ?: locales.values.firstOrNull { it is JsonObject } as? JsonObject
    }

    private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

    private fun JsonObject?.str(key: String): String? = (this?.get(key) as? JsonPrimitive)?.contentOrNull

    private fun JsonObject.bool(key: String): Boolean? = (this[key] as? JsonPrimitive)?.booleanOrNull

    private fun JsonObject.double(key: String): Double? = (this[key] as? JsonPrimitive)?.doubleOrNull
}
