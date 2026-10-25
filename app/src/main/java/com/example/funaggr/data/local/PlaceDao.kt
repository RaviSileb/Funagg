package com.example.funaggr.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

/** Geocoding results for venue/place names, so each name is resolved over the network only once. */
@Entity(tableName = "places")
data class PlaceEntity(
    @PrimaryKey val query: String,
    val latitude: Double,
    val longitude: Double,
    val resolvedAt: Long = System.currentTimeMillis()
)

@Dao
interface PlaceDao {
    @Query("SELECT * FROM places WHERE `query` = :query LIMIT 1")
    suspend fun find(query: String): PlaceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(place: PlaceEntity)
}
