package com.example.funaggr.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.funaggr.data.geo.CzechPlaces
import com.example.funaggr.data.geo.PlaceGeocoder
import com.example.funaggr.data.model.LocationData
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val geocoder: PlaceGeocoder
) {
    private val fusedClient by lazy { LocationServices.getFusedLocationProviderClient(context) }

    val defaultLocation = LocationData(50.0755, 14.4378, "Praha (výchozí poloha)", isFallback = true)

    fun hasPermission(): Boolean =
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION).any {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    /** Current device position, or null without permission or when no fix is available in time. */
    suspend fun currentLocation(): LocationData? {
        if (!hasPermission()) return null
        val location = freshLocation() ?: lastLocation() ?: return null
        val name = geocoder.describe(location.latitude, location.longitude)
            ?: CzechPlaces.nearest(location.latitude, location.longitude).name
        return LocationData(location.latitude, location.longitude, name)
    }

    @SuppressLint("MissingPermission")
    private suspend fun freshLocation(): Location? {
        val tokenSource = CancellationTokenSource()
        return try {
            withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
                val request = CurrentLocationRequest.Builder()
                    .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                    .setMaxUpdateAgeMillis(MAX_LOCATION_AGE_MS)
                    .setDurationMillis(LOCATION_TIMEOUT_MS)
                    .build()
                fusedClient.getCurrentLocation(request, tokenSource.token).await()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Current location unavailable: ${e.message}")
            null
        } finally {
            tokenSource.cancel()
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun lastLocation(): Location? =
        try {
            fusedClient.lastLocation.await()
        } catch (e: Exception) {
            Log.w(TAG, "Last location unavailable: ${e.message}")
            null
        }

    private companion object {
        const val TAG = "LocationProvider"
        const val LOCATION_TIMEOUT_MS = 10_000L
        const val MAX_LOCATION_AGE_MS = 5 * 60 * 1000L
    }
}
