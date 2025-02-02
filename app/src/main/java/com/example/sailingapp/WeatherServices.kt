package com.example.sailingapp

import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

suspend fun fetchWeatherDataWithGson(url: String): WeatherResponse {
    val response = withContext(Dispatchers.IO) {
        URL(url).readText() // Fetch the data from the API
    }
    return Gson().fromJson(response, WeatherResponse::class.java) // Parse JSON into data class
}

fun saveWeatherDataToFirebase(latitude: Double, longitude: Double, weatherData: String) {
    val database = FirebaseDatabase.getInstance()
    val ref = database.getReference("weather_data/${latitude}_${longitude}")
    ref.setValue(weatherData)
}

fun getWeatherDataFromFirebase(
    latitude: Double,
    longitude: Double,
    onSuccess: (String) -> Unit,
    onFailure: (Exception) -> Unit
) {
    val database = FirebaseDatabase.getInstance()
    val ref = database.getReference("weather_data/${latitude}_${longitude}")
    ref.get().addOnSuccessListener { snapshot ->
        val weatherData = snapshot.getValue(String::class.java)
        if (weatherData != null) {
            onSuccess(weatherData)
        } else {
            onFailure(Exception("No cached data found"))
        }
    }.addOnFailureListener { exception ->
        onFailure(exception)
    }
}