package com.example.sailingapp

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class LocationHelper(private val activity: Activity) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(activity)

    fun checkLocationPermission(onGranted: () -> Unit) {
        val permission = Manifest.permission.ACCESS_FINE_LOCATION
        if (ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED) {
            // If permission is already granted
            onGranted()
        } else {
            // Request location permission
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(permission),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

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
                Toast.makeText(activity, "Lat: ${location.latitude}, Lng: ${location.longitude}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(activity, "Unable to fetch location", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(activity, "Error fetching location: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
}