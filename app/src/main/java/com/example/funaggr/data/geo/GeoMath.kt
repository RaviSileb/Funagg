package com.example.funaggr.data.geo

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class GeoPoint(val latitude: Double, val longitude: Double)

object GeoMath {
    private const val EARTH_RADIUS_KM = 6371.0

    /** Great-circle distance (haversine) in kilometres. */
    fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
        return EARTH_RADIUS_KM * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    fun isValid(latitude: Double?, longitude: Double?): Boolean =
        latitude != null && longitude != null &&
            latitude in -90.0..90.0 && longitude in -180.0..180.0 &&
            !(latitude == 0.0 && longitude == 0.0)
}
