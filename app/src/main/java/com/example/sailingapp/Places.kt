package com.example.sailingapp

import android.content.Context
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient


object PlacesHelper {
    fun initializePlaces(context: Context): PlacesClient {
        if (!Places.isInitialized()) {
            Places.initialize(context, "AIzaSyBzoYcwXenBePtM1EGvS2mD14d3d_dfA4s")
        }
        return Places.createClient(context)
    }
}