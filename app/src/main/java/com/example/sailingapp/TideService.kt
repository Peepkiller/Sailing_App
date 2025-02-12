package com.example.sailingapp

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL


object TideService {
    private const val API_URL = "https://api.tidesandcurrents.noaa.gov/api/prod/datagetter?date=latest&station=8720218&product=water_level&datum=MLLW&time_zone=gmt&units=english&format=json"
    private val database = FirebaseDatabase.getInstance()
    private val tideRef = database.getReference("tide")

    suspend fun fetchTideData(): TideResponse? {
        return withContext(Dispatchers.IO) {
            // First, try to get data from Firebase
            try {
            val cachedData = getTideDataFromFirebase()
            if (cachedData != null) {
                Log.d("TideService", "Data retrieved from Firebase cache")
                return@withContext cachedData
            }

            // If not in Firebase, fetch from API
            Log.d("TideService", "Fetching data from API")
            val url = URL(API_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val tideData = Json.decodeFromString<TideResponse>(response)

                    // Save to Firebase
                    saveTideDataToFirebase(tideData)
                    tideData
                } else {
                    Log.e("TideService", "API request failed with code: $responseCode")
                    null
                }
            } catch (e: Exception) {
                Log.e("TideService", "Error fetching weather data", e)
                null
            }
        }
    }

    private fun saveTideDataToFirebase(tideData: TideResponse) {
        tideRef.setValue(tideData)
            .addOnSuccessListener { Log.d("TideService", "Tide data saved to Firebase") }
            .addOnFailureListener { e -> Log.e("TideService", "Failed to save tide data", e) }
    }

    private suspend fun getTideDataFromFirebase(): TideResponse? {
        return withContext(Dispatchers.IO) {
            var tideData: TideResponse? = null
            val latch = java.util.concurrent.CountDownLatch(1)

            tideRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    tideData = snapshot.getValue(TideResponse::class.java)
                    latch.countDown()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("TideService", "Error reading tide data from Firebase", error.toException())
                    latch.countDown()
                }
            })

            latch.await()
            tideData
        }
    }
}

@Serializable
data class TideResponse(
    @SerialName("metadata") val metadata: Metadata? = null,
    @SerialName("data") val data: List<TideDataPoint>? = null
)

@Serializable
data class Metadata(
    @SerialName("id") val stationId: String,
    @SerialName("name") val stationName: String
)

@Serializable
data class TideDataPoint(
    @SerialName("t") val time: String,
    @SerialName("v") val height: String
)
