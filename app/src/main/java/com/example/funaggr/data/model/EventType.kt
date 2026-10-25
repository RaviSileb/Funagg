package com.example.funaggr.data.model

enum class EventType(val displayName: String, val color: Long) {
    ALL("Vše", 0xFF6750A4),
    MUSIC("Hudba", 0xFFE91E63),
    THEATER("Divadlo", 0xFF9C27B0),
    MARKET("Trhy", 0xFF4CAF50),
    FESTIVAL("Festivaly", 0xFFFF9800),
    KIDS("Pro děti", 0xFF03A9F4),
    EXHIBITION("Výstavy", 0xFF3F51B5),
    SPORT("Sport", 0xFFFF5722),
    EDUCATION("Vzdělávání", 0xFF009688),
    OTHER("Ostatní", 0xFF607D8B);

    companion object {
        val filters: List<EventType> = entries.toList()
    }
}
