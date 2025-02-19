package com.example.sailingapp

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

suspend fun fetchWeatherDataWithGson(url: String): WeatherResponse {
    return withContext(Dispatchers.IO) {
        val json = URL(url).readText()
        Gson().fromJson(json, WeatherResponse::class.java)
    }
}

fun findClosestWeatherKey(database: DatabaseReference, generatedKey: String, onResult: (String?) -> Unit) {
    database.addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val availableKeys = snapshot.child("weather").children.mapNotNull { it.key }
            Log.d("WeatherDebug", "Available keys in Firebase: $availableKeys")

            // Check for an exact match first
            if (availableKeys.contains(generatedKey)) {
                Log.d("WeatherDebug", "Exact key match found: $generatedKey")
                onResult(generatedKey)
                return
            }

            // Find the closest match based on coordinate prefix
            val closestMatch = availableKeys.find { it.startsWith(generatedKey.substringBeforeLast("_dot_")) }

            Log.d("WeatherDebug", "Closest match found: $closestMatch")
            onResult(closestMatch ?: generatedKey) // If nothing found, use original key
        }

        override fun onCancelled(error: DatabaseError) {
            Log.e("WeatherDebug", "Error fetching weather keys", error.toException())
            onResult(null)
        }
    })
}
