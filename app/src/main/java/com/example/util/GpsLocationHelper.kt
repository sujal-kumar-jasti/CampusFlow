package com.example.util

import android.location.Location

object GpsLocationHelper {

    data class CampusLocation(
        val name: String = "IIEST, Shibpur",
        val latitude: Double = 22.555123,
        val longitude: Double = 88.307615,
        val radiusMeters: Double = 26.0
    )

    /**
     * Calculates distance between two coordinates using android.location.Location.distanceBetween
     * for high precision (WGS84 ellipsoid).
     */
    fun calculateDistanceMeters(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0].toDouble()
    }

    data class NearbyCollege(
        val name: String,
        val distanceMeters: Double,
        val latitude: Double,
        val longitude: Double,
        val address: String
    )

    /**
     * Finds nearby colleges based on current location.
     */
    fun findNearbyColleges(
        @Suppress("UNUSED_PARAMETER") context: android.content.Context,
        lat: Double,
        lon: Double
    ): List<NearbyCollege> {
        val colleges = listOf(
            NearbyCollege("IIEST Shibpur", 0.0, 22.5552, 88.3075, "Howrah, West Bengal"),
            NearbyCollege("IIT Kharagpur", 0.0, 22.3149, 87.3105, "Kharagpur, West Bengal"),
            NearbyCollege("Jadavpur University", 0.0, 22.4992, 88.3709, "Kolkata, West Bengal"),
            NearbyCollege("NIT Durgapur", 0.0, 23.5477, 87.2931, "Durgapur, West Bengal")
        )
        
        return colleges.map { col ->
            val dist = calculateDistanceMeters(lat, lon, col.latitude, col.longitude)
            col.copy(distanceMeters = dist)
        }.sortedBy { it.distanceMeters }.filter { it.distanceMeters < 50000 } // Within 50km
    }
}
