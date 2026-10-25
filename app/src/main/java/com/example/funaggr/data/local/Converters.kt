package com.example.funaggr.data.local

import androidx.room.TypeConverter
import com.example.funaggr.data.model.EventType

class Converters {
    @TypeConverter
    fun fromEventType(value: EventType): String = value.name

    @TypeConverter
    fun toEventType(value: String): EventType =
        EventType.entries.firstOrNull { it.name == value } ?: EventType.OTHER
}
