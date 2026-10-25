package com.example.funaggr.data.remote

object TextParsing {
    private val numberRegex = Regex("\\d[\\d\\u00A0 ]*")
    private val freeRegex = Regex("zdarma|vstup volný|volný vstup|free", RegexOption.IGNORE_CASE)

    /** Lowest price mentioned in [text]; 0 when the text says the event is free. */
    fun parsePriceCzk(text: String?): Int? {
        if (text.isNullOrBlank()) return null
        if (freeRegex.containsMatchIn(text)) return 0
        return numberRegex.find(text)?.value?.filter { it.isDigit() }?.toIntOrNull()
    }

    /** Converts lightweight Markdown (as used by GoOut) to plain text and shortens it. */
    fun plainText(markdown: String?, maxLength: Int = 400): String {
        if (markdown.isNullOrBlank()) return ""
        val text = markdown
            .replace(Regex("\\[([^\\]]*)]\\([^)]*\\)"), "$1")
            .replace(Regex("[*_#>`~]+"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        return if (text.length <= maxLength) text else text.take(maxLength).substringBeforeLast(' ') + "…"
    }
}
