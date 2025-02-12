package com.example.sailingapp

import kotlin.math.sqrt

data class TideStation(val id: String, val name: String, val lat: Double, val lon: Double)

// Predefined NOAA tide stations
val tideStations = listOf(
    TideStation("8720218", "Mayport, FL", 30.3967, -81.4306),
    TideStation("8721604", "Daytona Beach, FL", 29.2103, -81.0114),
    TideStation("8722670", "Miami Beach, FL", 25.7781, -80.1325)
)

fun findNearestTideStation(userLat: Double, userLon: Double): TideStation? {
    return tideStations.minByOrNull { station ->
        val latDiff = userLat - station.lat
        val lonDiff = userLon - station.lon
        sqrt(latDiff * latDiff + lonDiff * lonDiff)
    }
}