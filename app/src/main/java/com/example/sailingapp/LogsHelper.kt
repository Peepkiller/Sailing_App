package com.example.sailingapp

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


data class RoutePoint(
    val lat: Double = 0.0,
    val lng: Double = 0.0
)

data class TripLog(
    val id: String = "",
    val title: String = "",
    val notes: String = "",
    val routePoints: List<RoutePoint> = emptyList(),
    val timestamp: Long = 0,
    val startLocation: String = "",
    val endLocation: String = ""
)

object FirebaseLogHelper {

    private val database = FirebaseDatabase.getInstance().getReference("logs")

    fun saveLog(
        title: String,
        notes: String,
        routePoints: List<LatLng>,
        startLocation: String,
        endLocation: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val logId = database.push().key ?: return
        val logData = mapOf(
            "id" to logId,
            "title" to title,
            "notes" to notes,
            "routePoints" to routePoints.map { RoutePoint(it.latitude, it.longitude)  },
            "timestamp" to System.currentTimeMillis(),
            "startLocation" to startLocation,
            "endLocation" to endLocation
        )

        database.child(logId).setValue(logData)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun getLogs(onLogsFetched: (List<TripLog>) -> Unit, onFailure: (Exception) -> Unit) {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val logs = snapshot.children.mapNotNull {
                    try {
                        val log = it.getValue(TripLog::class.java)
                        log?.copy(id = it.key ?: "")
                    } catch (e: Exception) {
                        Log.e("Firebase", "Error parsing log: ${e.message}")
                        null
                    }
                }
                onLogsFetched(logs)
            }

            override fun onCancelled(error: DatabaseError) {
                onFailure(error.toException())
            }
        })
    }

    fun deleteLog(logId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        database.child(logId).removeValue()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { error -> onFailure(error) }
    }
}



