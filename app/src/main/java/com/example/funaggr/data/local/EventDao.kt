package com.example.funaggr.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.funaggr.data.model.Event
import kotlinx.coroutines.flow.Flow

@Dao
abstract class EventDao {
    @Query("SELECT * FROM events ORDER BY distanceKm IS NULL, distanceKm ASC, dateStart ASC")
    abstract fun observeAll(): Flow<List<Event>>

    @Query("SELECT * FROM events")
    abstract suspend fun getAll(): List<Event>

    @Query("SELECT COUNT(*) FROM events")
    abstract suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(events: List<Event>)

    @Query("DELETE FROM events")
    abstract suspend fun deleteAll()

    /** Atomically swaps the cached events so observers never see an empty intermediate list. */
    @Transaction
    open suspend fun replaceAll(events: List<Event>) {
        deleteAll()
        insertAll(events)
    }
}
