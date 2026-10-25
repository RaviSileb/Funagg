package com.example.funaggr.data.remote

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URI
import java.time.LocalDate

/**
 * Parses the kudyznudy.cz event calendar.
 *
 * The page marks events up with schema.org microdata. When that markup disappears the parser
 * falls back to JSON-LD and finally to plain `/akce/` links, so a redesign degrades the data
 * instead of silently yielding nothing.
 */
object KudyZNudyParser {

    data class Item(
        val title: String,
        val url: String,
        val startDate: LocalDate?,
        val endDate: LocalDate?,
        val description: String,
        val place: String,
        val imageUrl: String?
    )

    data class Page(val items: List<Item>, val totalPages: Int, val strategy: String)

    private val pageRegex = Regex("[?&]stranka=(\\d+)")
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun parse(html: String, baseUrl: String): Page {
        val document = Jsoup.parse(html, baseUrl)
        val totalPages = document.select("a[href*=stranka=]")
            .mapNotNull { pageRegex.find(it.attr("href"))?.groupValues?.get(1)?.toIntOrNull() }
            .maxOrNull() ?: 1

        parseMicrodata(document).takeIf { it.isNotEmpty() }?.let { return Page(it, totalPages, "microdata") }
        parseJsonLd(document).takeIf { it.isNotEmpty() }?.let { return Page(it, totalPages, "json-ld") }
        return Page(parseLinks(document), totalPages, "links")
    }

    private fun parseMicrodata(document: Document): List<Item> =
        document.select("[itemtype*=schema.org/Event]").mapNotNull { element ->
            val link = element.selectFirst("h3 a[href], h2 a[href], a[href*=/akce/]") ?: return@mapNotNull null
            val title = element.selectFirst("h3 [itemprop=name], h2 [itemprop=name]")?.text()?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: link.attr("title").trim().takeIf { it.isNotEmpty() }
                ?: link.text().trim().takeIf { it.isNotEmpty() }
                ?: return@mapNotNull null
            Item(
                title = title,
                url = link.absUrl("href").ifEmpty { return@mapNotNull null },
                startDate = parseDate(element.metaContent("startDate")),
                endDate = parseDate(element.metaContent("endDate")),
                description = element.metaContent("description").orEmpty().trim(),
                place = element.selectFirst("[itemprop=location] [itemprop=name], .location")?.text()?.trim().orEmpty(),
                imageUrl = element.selectFirst("img[itemprop=image], img[src]")?.absUrl("src")?.ifEmpty { null }
            )
        }

    private fun parseJsonLd(document: Document): List<Item> =
        document.select("script[type=application/ld+json]").flatMap { script ->
            val root = runCatching { json.parseToJsonElement(script.data()) }.getOrNull() ?: return@flatMap emptyList()
            flatten(root).filter { it.string("@type")?.contains("Event") == true }.mapNotNull { obj ->
                val title = obj.string("name") ?: return@mapNotNull null
                val url = obj.string("url")?.let { document.absolute(it) } ?: return@mapNotNull null
                val location = obj["location"]
                val place = when (location) {
                    is JsonObject -> listOfNotNull(
                        location.string("name"),
                        (location["address"] as? JsonObject)?.string("addressLocality")
                    ).joinToString(", ")
                    is JsonPrimitive -> location.contentOrNull.orEmpty()
                    else -> ""
                }
                Item(
                    title = title,
                    url = url,
                    startDate = parseDate(obj.string("startDate")),
                    endDate = parseDate(obj.string("endDate")),
                    description = obj.string("description").orEmpty(),
                    place = place,
                    imageUrl = obj.string("image")
                )
            }
        }

    private fun parseLinks(document: Document): List<Item> =
        document.select("a[href*=/akce/]")
            .filter { it.parents().none { p -> p.tagName() == "nav" || p.tagName() == "footer" } }
            .mapNotNull { link ->
                val title = link.attr("title").ifBlank { link.text() }.trim()
                val url = link.absUrl("href")
                if (title.length < 3 || url.isEmpty()) null
                else Item(title, url, null, null, "", "", null)
            }
            .distinctBy { it.url }

    private fun flatten(element: JsonElement): List<JsonObject> = when (element) {
        is JsonArray -> element.flatMap { flatten(it) }
        is JsonObject -> listOf(element) + (element["@graph"]?.let { flatten(it) } ?: emptyList())
        else -> emptyList()
    }

    private fun JsonObject.string(key: String): String? = when (val value = this[key]) {
        is JsonPrimitive -> value.contentOrNull
        is JsonArray -> value.firstOrNull()?.let { (it as? JsonPrimitive)?.contentOrNull ?: (it as? JsonObject)?.string("url") }
        is JsonObject -> value.string("url")
        else -> null
    }

    private fun Document.absolute(url: String): String =
        if (url.startsWith("http")) url else runCatching { URI(location()).resolve(url).toString() }.getOrDefault(url)

    private fun Element.metaContent(itemprop: String): String? =
        selectFirst("meta[itemprop=$itemprop]")?.attr("content")?.takeIf { it.isNotBlank() }

    private fun parseDate(value: String?): LocalDate? =
        value?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
}
