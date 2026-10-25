package com.example.funaggr.data.geo

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.util.Log
import com.example.funaggr.data.local.PlaceDao
import com.example.funaggr.data.local.PlaceEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Collections
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

interface PlaceGeocoder {
    /** Coordinates of a place name, or null when it cannot be resolved. */
    suspend fun locate(place: String): GeoPoint?

    /** Human readable name of the place at the given coordinates, or null. */
    suspend fun describe(latitude: Double, longitude: Double): String?
}

/**
 * Resolves place names in this order: offline gazetteer, Room cache, Android [Geocoder].
 * Misses are remembered for the process lifetime so a failing name costs one lookup only.
 */
@Singleton
class CachingPlaceGeocoder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val placeDao: PlaceDao
) : PlaceGeocoder {

    private val misses: MutableSet<String> = Collections.synchronizedSet(HashSet())
    private val geocoder by lazy { Geocoder(context, Locale.forLanguageTag("cs-CZ")) }

    override suspend fun locate(place: String): GeoPoint? {
        val key = CzechPlaces.normalize(place)
        if (key.isEmpty() || key in misses) return null

        CzechPlaces.lookup(place)?.let { return GeoPoint(it.latitude, it.longitude) }
        placeDao.find(key)?.let { return GeoPoint(it.latitude, it.longitude) }

        val address = withTimeoutOrNull(GEOCODER_TIMEOUT_MS) { geocodeName("$place, Česko") }
        if (address == null || !address.hasLatitude() || !address.hasLongitude()) {
            misses += key
            return null
        }
        val point = GeoPoint(address.latitude, address.longitude)
        placeDao.insert(PlaceEntity(key, point.latitude, point.longitude))
        return point
    }

    override suspend fun describe(latitude: Double, longitude: Double): String? {
        val address = withTimeoutOrNull(GEOCODER_TIMEOUT_MS) { reverseGeocode(latitude, longitude) }
            ?: return null
        val locality = address.locality ?: address.subAdminArea ?: return null
        val subLocality = address.subLocality?.takeIf { it != locality }
        return if (subLocality != null) "$locality – $subLocality" else locality
    }

    private suspend fun geocodeName(query: String): Address? {
        if (!Geocoder.isPresent()) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { cont ->
                geocoder.getFromLocationName(query, 1, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        if (cont.isActive) cont.resume(addresses.firstOrNull())
                    }

                    override fun onError(errorMessage: String?) {
                        Log.w(TAG, "Geocoding '$query' failed: $errorMessage")
                        if (cont.isActive) cont.resume(null)
                    }
                })
            }
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocationName(query, 1)?.firstOrNull()
                }.getOrNull()
            }
        }
    }

    private suspend fun reverseGeocode(latitude: Double, longitude: Double): Address? {
        if (!Geocoder.isPresent()) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { cont ->
                geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        if (cont.isActive) cont.resume(addresses.firstOrNull())
                    }

                    override fun onError(errorMessage: String?) {
                        if (cont.isActive) cont.resume(null)
                    }
                })
            }
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
                }.getOrNull()
            }
        }
    }

    private companion object {
        const val TAG = "PlaceGeocoder"
        const val GEOCODER_TIMEOUT_MS = 6_000L
    }
}
