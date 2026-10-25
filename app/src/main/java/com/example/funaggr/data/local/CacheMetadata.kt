package com.example.funaggr.data.local

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Where and when the cached events were fetched; used to decide whether the cache is reusable. */
data class FetchMeta(
    val fetchedAt: Long,
    val latitude: Double,
    val longitude: Double,
    val radiusKm: Int
)

interface CacheMetadata {
    fun read(): FetchMeta?
    fun write(meta: FetchMeta)
}

@Singleton
class PrefsCacheMetadata @Inject constructor(
    @ApplicationContext context: Context
) : CacheMetadata {
    private val prefs = context.getSharedPreferences("fetch_meta", Context.MODE_PRIVATE)

    override fun read(): FetchMeta? {
        val fetchedAt = prefs.getLong(KEY_FETCHED_AT, 0L)
        if (fetchedAt == 0L) return null
        return FetchMeta(
            fetchedAt = fetchedAt,
            latitude = prefs.getFloat(KEY_LAT, 0f).toDouble(),
            longitude = prefs.getFloat(KEY_LON, 0f).toDouble(),
            radiusKm = prefs.getInt(KEY_RADIUS, 0)
        )
    }

    override fun write(meta: FetchMeta) {
        prefs.edit {
            putLong(KEY_FETCHED_AT, meta.fetchedAt)
            putFloat(KEY_LAT, meta.latitude.toFloat())
            putFloat(KEY_LON, meta.longitude.toFloat())
            putInt(KEY_RADIUS, meta.radiusKm)
        }
    }

    private companion object {
        const val KEY_FETCHED_AT = "fetched_at"
        const val KEY_LAT = "lat"
        const val KEY_LON = "lon"
        const val KEY_RADIUS = "radius"
    }
}
