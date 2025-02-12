package com.example.sailingapp

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

suspend fun fetchWeatherDataWithGson(url: String): WeatherResponse {
    val response = withContext(Dispatchers.IO) {
        URL(url).readText() // Fetch the data from the API
    }
    return Gson().fromJson(response, WeatherResponse::class.java) // Parse JSON
}

