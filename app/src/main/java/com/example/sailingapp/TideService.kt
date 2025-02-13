package com.example.sailingapp

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume



object TideService {
    private const val BASE_URL = "https://api.tidesandcurrents.noaa.gov/api/prod/datagetter"

    private val database = FirebaseDatabase.getInstance()
    private val tideRef = database.getReference("tide")

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchTideData(stationId: String = "8720218"): TideResponse? {
        return withContext(Dispatchers.IO) {
            // First, try to get data from Firebase
            try {
                val cachedData = getTideDataFromFirebase(stationId)
                if (cachedData != null) {
                    Log.d("TideService", "Data retrieved from Firebase cache")
                    return@withContext cachedData
                }

                // If not in Firebase, fetch from API
                Log.d("TideService", "Fetching data from station $stationId")
                val url =
                    URL("$BASE_URL?date=latest&station=$stationId&product=water_level&datum=MLLW&time_zone=gmt&units=english&format=json")

                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val tideData = json.decodeFromString<TideResponse>(response)

                    // Save to Firebase
                    saveTideDataToFirebase(stationId, tideData)
                    tideData
                } else {
                    Log.e("TideService", "API request failed with code: $responseCode")
                    null
                }
            } catch (e: Exception) {
                Log.e("TideService", "Error fetching Tide data", e)
                null
            }
        }
    }

    private fun saveTideDataToFirebase(stationId: String, tideData: TideResponse) {
        tideRef.child(stationId).setValue(tideData)
            .addOnSuccessListener { Log.d("TideService", "Tide data saved to Firebase") }
            .addOnFailureListener { e -> Log.e("TideService", "Failed to save tide data", e) }
    }

    private suspend fun getTideDataFromFirebase(stationId: String): TideResponse? {
        return withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { continuation ->
                tideRef.child(stationId)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val tideData = snapshot.getValue(TideResponse::class.java)
                            continuation.resume(tideData)
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e(
                                "TideService",
                                "Error reading tide data from Firebase",
                                error.toException()
                            )
                            continuation.resume(null)
                        }
                    })
            }
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
    @SerialName("id") val stationId: String = "",
    @SerialName("name") val stationName: String = ""
){
    constructor() : this("", "")
}

@Serializable
data class TideDataPoint(
    @SerialName("t") val time: String = "",
    @SerialName("v") val height: String = ""
) {
    constructor() : this("", "")
}
