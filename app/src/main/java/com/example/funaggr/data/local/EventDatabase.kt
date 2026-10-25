package com.example.funaggr.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.funaggr.data.model.Event

@Database(entities = [Event::class, PlaceEntity::class], version = 3, exportSchema = false)
@TypeConverters(Converters::class)
abstract class EventDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun placeDao(): PlaceDao

    companion object {
        const val NAME = "funaggr.db"
    }
}
