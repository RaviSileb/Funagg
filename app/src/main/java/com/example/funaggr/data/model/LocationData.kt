package com.example.funaggr.data.model

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val cityName: String = "Neznámá lokace",
    /** True when this is the built-in fallback rather than a real device fix. */
    val isFallback: Boolean = false
)
