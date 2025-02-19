package com.example.sailingapp

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

class LocationHelper(private val activity: Activity) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(activity)

    fun fetchLocation(onLocationFetched: (Double, Double) -> Unit) {
        if (ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(activity, "Location permission not granted", Toast.LENGTH_SHORT).show()
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                onLocationFetched(location.latitude, location.longitude)
                Toast.makeText(
                    activity,
                    "Lat: ${location.latitude}, Lng: ${location.longitude}",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(activity, "Unable to fetch location", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(activity, "Error fetching location: ${it.message}", Toast.LENGTH_SHORT)
                .show()
        }
    }
}


    suspend fun getDirections(origin: LatLng, destination: LatLng): List<LatLng> {
        val apiKey = "AIzaSyBzoYcwXenBePtM1EGvS2mD14d3d_dfA4s"
        val url = "https://maps.googleapis.com/maps/api/directions/json?origin=${origin.latitude},${origin.longitude}&destination=${destination.latitude},${destination.longitude}&key=$apiKey"

        return withContext(Dispatchers.IO) {
            try {
                val response = URL(url).readText()
                Log.d("Directions", response)

                val jsonObject = JSONObject(response)
                val routes = jsonObject.getJSONArray("routes")

                if (routes.length() == 0) {
                    Log.e("Directions", "No routes found")
                    return@withContext emptyList()
                }

                val polyline = routes.getJSONObject(0)
                    .getJSONObject("overview_polyline")
                    .getString("points")

                Log.d("Polyline", polyline)

                decodePolyline(polyline)

            } catch (e: Exception) {
                Log.e("Directions", "Error fetching directions: ${e.message}")
                emptyList()
            }
        }
    }

fun decodePolyline(encoded: String): List<LatLng> {
    val polyline = mutableListOf<LatLng>()
    var index = 0
    val len = encoded.length
    var lat = 0
    var lng = 0

    while (index < len) {
        var b: Int
        var shift = 0
        var result = 0
        do {
            b = encoded[index++].code - 63
            result = result or ((b and 0x1F) shl shift)
            shift += 5
        } while (b >= 0x20)
        val deltaLat = if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)
        lat += deltaLat

        shift = 0
        result = 0
        do {
            b = encoded[index++].code - 63
            result = result or ((b and 0x1F) shl shift)
            shift += 5
        } while (b >= 0x20)
        val deltaLng = if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)
        lng += deltaLng

        polyline.add(LatLng(lat / 1E5, lng / 1E5))
    }

    return polyline
}


