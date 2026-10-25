package com.example.funaggr.data.remote

import com.example.funaggr.data.model.EventType

/**
 * Infers [EventType] from source categories and Czech keywords.
 *
 * Keywords are matched at word starts only, so e.g. "hra" no longer matches "hrad" or "zahrada".
 */
object EventClassifier {

    private fun words(vararg patterns: String) =
        Regex("(?<![\\p{L}\\p{N}])(?:${patterns.joinToString("|")})", RegexOption.IGNORE_CASE)

    private val rules: List<Pair<EventType, Regex>> = listOf(
        EventType.MARKET to words("trh", "jarmark", "market", "bleší", "blešák", "burz", "farmářsk"),
        EventType.KIDS to words("pro děti", "dět", "pohád", "rodinn", "loutk", "kids"),
        EventType.FESTIVAL to words("festival", "fest\\b", "slavnost", "pouť", "posvícen", "dožínk"),
        EventType.THEATER to words(
            "divadl", "představení", "činohr", "muzikál", "balet", "oper[auy]\\b", "stand-?up",
            "comedy", "kabaret", "improvizac", "inscenac"
        ),
        EventType.MUSIC to words(
            "koncert", "hudb", "hudeb", "jazz", "rock", "punk", "metal", "dj\\b", "kapel",
            "orchestr", "filharmon", "recitál", "sbor", "party", "rave", "techno", "hip-hop", "swing"
        ),
        EventType.EXHIBITION to words("výstav", "galeri", "expozic", "vernisáž", "exhibition", "muze", "museum"),
        EventType.SPORT to words(
            "sport", "běh\\b", "běhu\\b", "běžeck", "závod", "zápas", "turnaj", "maraton",
            "dostih", "hokej", "fotbal", "cyklo", "tenis", "golf", "lyž"
        ),
        EventType.EDUCATION to words(
            "přednášk", "besed", "workshop", "seminář", "kurz", "dílna", "dílnič",
            "komentovan", "prohlídk", "exkurz", "vycházk"
        )
    )

    private val goOutCategories = mapOf(
        "concerts" to EventType.MUSIC,
        "clubbing" to EventType.MUSIC,
        "play" to EventType.THEATER,
        "theater" to EventType.THEATER,
        "festivals" to EventType.FESTIVAL,
        "exhibitions" to EventType.EXHIBITION,
        "gallery" to EventType.EXHIBITION,
        "sport" to EventType.SPORT,
        "talks" to EventType.EDUCATION,
        "workshops" to EventType.EDUCATION
    )

    fun classify(title: String, description: String = "", categories: List<String> = emptyList()): EventType {
        if ("for_children" in categories) return EventType.KIDS
        categories.firstNotNullOfOrNull { goOutCategories[it] }?.let { return it }
        return byKeywords(title) ?: byKeywords(description) ?: EventType.OTHER
    }

    private fun byKeywords(text: String): EventType? {
        if (text.isBlank()) return null
        return rules.firstOrNull { (_, regex) -> regex.containsMatchIn(text) }?.first
    }
}
