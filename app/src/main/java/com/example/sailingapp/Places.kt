package com.example.sailingapp

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


object PlacesHelper {
    fun initializePlaces(context: Context): PlacesClient {
        if (!Places.isInitialized()) {
            Places.initialize(context, "AIzaSyBzoYcwXenBePtM1EGvS2mD14d3d_dfA4s")
        }
        return Places.createClient(context)
    }

    fun getPlaceDetails(placeId: String, placesClient: PlacesClient, onPlaceFound: (LatLng?) -> Unit) {
        val placeRequest = FetchPlaceRequest.builder(
            placeId,
            listOf(Place.Field.LAT_LNG)
        ).build()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val placeResponse = placesClient.fetchPlace(placeRequest).await()
                val latLng = placeResponse.place.latLng
                withContext(Dispatchers.Main) {
                    println("Place found: $latLng")
                    onPlaceFound(latLng)
                }
            } catch (e: Exception) {
                println("Error fetching place details: ${e.message}")
                onPlaceFound(null)
            }
        }
    }
}
