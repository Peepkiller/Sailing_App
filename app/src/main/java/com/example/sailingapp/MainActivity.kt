package com.example.sailingapp

import android.app.Activity
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.sailingapp.ui.theme.SailingAppTheme
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.firebase.database.FirebaseDatabase
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class MainActivity : ComponentActivity() {

    private lateinit var locationHelper: LocationHelper
    private lateinit var placesClient: PlacesClient
    private var userLatitude = 0.0
    private var userLongitude = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseDatabase.getInstance().setPersistenceEnabled(true)

        // Initialize PlacesClient
        placesClient = PlacesHelper.initializePlaces(this, "AIzaSyBzoYcwXenBePtM1EGvS2mD14d3d_dfA4s")


        // Register permission launcher
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // Fetch location if permission is granted
                locationHelper.fetchLocation { latitude, longitude ->
                    userLatitude = latitude
                    userLongitude = longitude
                }
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }

        // Initialize LocationHelper
        locationHelper = LocationHelper(this)

        // Check permissions and fetch location
        locationHelper.checkLocationPermission {
            locationHelper.fetchLocation { latitude, longitude ->
                userLatitude = latitude
                userLongitude = longitude
            }
        }

        // Set up UI
        setContent {
            SailingAppTheme {
                MainScreen(
                    userLatitude = userLatitude,
                    userLongitude = userLongitude,
                    placesClient = placesClient
                )
            }
        }
    }
}


    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainScreen(userLatitude: Double, userLongitude: Double, placesClient: PlacesClient) {
        var selectedTab by remember { mutableIntStateOf(0) } // State for tab selection
        var searchQuery by remember { mutableStateOf("") } // State for search query
        var moreScreenSubpage by remember { mutableStateOf("") } // Tracks subpage navigation in MoreScreen
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(LatLng(userLatitude, userLongitude), 15f) // Initialize camera position
        }

        Scaffold(
            topBar = {
                if (selectedTab == 0) { // Only show search bar on GPS & Logs tab
                    TopAppBar(
                        title = {},
                        actions = {
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search places...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            )
                        }
                    )
                }
            },
            bottomBar = {
                BottomAppBar {
                    IconButton(onClick = { selectedTab = 0 }) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = "GPS & Logs"
                        )
                    }
                    IconButton(onClick = { selectedTab = 1 }) { // Weather tab
                        Icon(imageVector = Icons.Filled.Cloud, contentDescription = "Weather")
                    }
                    IconButton(onClick = { selectedTab = 2 }) { // More features tab
                        Icon(imageVector = Icons.Filled.MoreHoriz, contentDescription = "More")
                    }
                }
            }
        ) { innerPadding ->
            when (selectedTab) {
                0 -> GpsLogsScreen(
                    modifier = Modifier.padding(innerPadding),
                    userLatitude = userLatitude,
                    userLongitude = userLongitude,
                    searchQuery = searchQuery,
                    placesClient = placesClient,
                    cameraPositionState = cameraPositionState
                )

                1 -> WeatherScreen(
                    modifier = Modifier.padding(innerPadding),
                    latitude = userLatitude,
                    longitude = userLongitude
                )

                2 -> if (moreScreenSubpage.isEmpty()) {
                    MoreScreen(
                        modifier = Modifier.padding(innerPadding),
                        onNavigate = { moreScreenSubpage = it } // Navigate to specific subpages
                    )
                } else {
                    when (moreScreenSubpage) {
                        "compass" -> CompassScreen(onBack = { moreScreenSubpage = "" })
                        "calculator" -> SpeedDistanceCalculatorScreen(onBack = { moreScreenSubpage = "" })
                        "logs" -> LogsScreen(onBack = { moreScreenSubpage = "" })
                    }
                }
            }
        }
    }

    @Composable
    fun GpsLogsScreen(
        modifier: Modifier = Modifier,
        userLatitude: Double,
        userLongitude: Double,
        searchQuery: String,
        placesClient: PlacesClient,
        cameraPositionState: CameraPositionState
    ) {
        var lat by remember { mutableDoubleStateOf(userLatitude) }
        var lng by remember { mutableDoubleStateOf(userLongitude) }
        var isLocationInitialized by remember { mutableStateOf(false) }
        val context = LocalContext.current
        val locationHelper = remember { LocationHelper(context as Activity) }

        // Initialize location on app launch
        LaunchedEffect(Unit) {
            locationHelper.fetchLocation { latitude, longitude ->
                lat = latitude
                lng = longitude
                cameraPositionState.position = CameraPosition.fromLatLngZoom(LatLng(lat, lng), 15f)
                isLocationInitialized = true
            }
        }

        Box(modifier = modifier.fillMaxSize()) {
            // Google Map setup
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(
                    compassEnabled = true,
                    myLocationButtonEnabled = false
                )
            ) {
                Marker(
                    state = MarkerState(position = LatLng(lat, lng)),
                    title = "You are here"
                )
            }

            // Floating Action Button to fetch and update location
            FloatingActionButton(
                onClick = {
                    locationHelper.fetchLocation { latitude, longitude ->
                        lat = latitude
                        lng = longitude
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(LatLng(lat, lng), 15f)
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Icon(imageVector = Icons.Filled.LocationOn, contentDescription = "Precise Location")
            }

            // Search bar logic for places
            LaunchedEffect(searchQuery) {
                if (searchQuery.isNotEmpty()) {
                    val request = FindAutocompletePredictionsRequest.builder()
                        .setQuery(searchQuery)
                        .build()

                    try {
                        val response = placesClient.findAutocompletePredictions(request).await()
                        val predictions = response.autocompletePredictions

                        if (predictions.isNotEmpty()) {
                            val firstPrediction = predictions.first()
                            val placeId = firstPrediction.placeId

                            val placeRequest = FetchPlaceRequest.builder(
                                placeId,
                                listOf(Place.Field.LOCATION)
                            ).build()

                            val placeResponse = placesClient.fetchPlace(placeRequest).await()
                            val latLng = placeResponse.place.location

                            if (latLng != null) {
                                cameraPositionState.position =
                                    CameraPosition.fromLatLngZoom(latLng, 15f)
                            }
                        }
                    } catch (e: Exception) {
                        println("Error fetching places: ${e.message}")
                    }
                }
            }
        }
    }

    @Composable
    fun WeatherScreen(modifier: Modifier = Modifier, latitude: Double, longitude: Double) {
        var weatherData by remember { mutableStateOf<String?>(null) }
        var isLoading by remember { mutableStateOf(true) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        val coroutineScope = rememberCoroutineScope()

        // Fetch weather data when the screen is loaded
        LaunchedEffect(Unit) {
            coroutineScope.launch {
                isLoading = true
                try {
                    val url =
                        "https://api.open-meteo.com/v1/forecast?latitude=28.5383&longitude=-81.3792&current=temperature_2m,precipitation,wind_speed_10m,wind_direction_10m&hourly=temperature_2m,precipitation_probability,rain,visibility,wind_speed_10m&daily=temperature_2m_max,temperature_2m_min,sunrise,sunset&temperature_unit=fahrenheit&wind_speed_unit=kn&precipitation_unit=inch&timezone=GMT"

                    val weatherResponse = fetchWeatherDataWithGson(url)

                    weatherData = """
                    Latitude: ${weatherResponse.latitude}
                    Longitude: ${weatherResponse.longitude}
                    Temperature (First Hour): ${weatherResponse.hourly.temperature_2m[0]} ${weatherResponse.hourly_units.temperature_2m}
                    Precipitation Probability: ${weatherResponse.hourly.precipitation_probability[0]}%
                    Rain: ${weatherResponse.hourly.rain[0]} ${weatherResponse.hourly_units.rain}
                    Wind Speed: ${weatherResponse.hourly.wind_speed_10m[0]} ${weatherResponse.hourly_units.wind_speed_10m}
                    Max Temp Today: ${weatherResponse.daily.temperature_2m_max[0]} ${weatherResponse.daily_units.temperature_2m_max}
                    Min Temp Today: ${weatherResponse.daily.temperature_2m_min[0]} ${weatherResponse.daily_units.temperature_2m_min}
                    Sunrise: ${weatherResponse.daily.sunrise[0]}
                    Sunset: ${weatherResponse.daily.sunset[0]}
                """.trimIndent()
                } catch (e: Exception) {
                    // Attempt to retrieve cached data from Firebase if there's an error
                    getWeatherDataFromFirebase(
                        latitude = latitude,
                        longitude = longitude,
                        onSuccess = { cachedData ->
                            weatherData = cachedData
                        },
                        onFailure = { error ->
                            errorMessage = error.message ?: "Failed to fetch weather data"
                        }
                    )
                } finally {
                    isLoading = false
                }
            }
        }

        // UI rendering
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            when {
                isLoading -> Text("Loading weather data...", Modifier.align(Alignment.Center))
                errorMessage != null -> Text("Error: $errorMessage", Modifier.align(Alignment.Center))
                weatherData != null -> {
                    Column {
                        Text(
                            text = "Weather Data:",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = weatherData ?: "No data",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun MoreScreen(modifier: Modifier = Modifier, onNavigate: (String) -> Unit) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Marine Navigation Tools",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Button(onClick = { onNavigate("compass") }, modifier = Modifier.fillMaxWidth()) {
                Text("Compass")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = { onNavigate("calculator") }, modifier = Modifier.fillMaxWidth()) {
                Text("Speed/Distance Calculator")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = { onNavigate("logs") }, modifier = Modifier.fillMaxWidth()) {
                Text("Trip Logs")
            }
        }
    }

@Composable
fun CompassScreen(onBack: () -> Unit) {
    val heading = remember { mutableFloatStateOf(0f) } // Holds the compass heading in degrees
    val sensorManager = LocalContext.current.getSystemService(SensorManager::class.java)
    val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    val magnetometer = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    val accelerometerValues = remember { FloatArray(3) }
    val magnetometerValues = remember { FloatArray(3) }
    val rotationMatrix = FloatArray(9)
    val orientationAngles = FloatArray(3)

    DisposableEffect(Unit) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor == accelerometer) {
                    if (event != null) {
                        System.arraycopy(event.values, 0, accelerometerValues, 0, event.values.size)
                    }
                } else if (event?.sensor == magnetometer) {
                    if (event != null) {
                        System.arraycopy(event.values, 0, magnetometerValues, 0, event.values.size)
                    }
                }

                if (SensorManager.getRotationMatrix(
                        rotationMatrix,
                        null,
                        accelerometerValues,
                        magnetometerValues
                    )
                ) {
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)
                    var azimuthInDegrees = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()

                    // Normalize the heading to be within 0° to 360°
                    if (azimuthInDegrees < 0) {
                        azimuthInDegrees += 360
                    }
                    heading.floatValue = azimuthInDegrees
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager?.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        sensorManager?.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_UI)

        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Compass",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Box(
            modifier = Modifier
                .size(200.dp)
                .background(MaterialTheme.colorScheme.primary, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${heading.floatValue.toInt()}°",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onBack) {
            Text("Back")
        }
    }
}

@Composable
fun SpeedDistanceCalculatorScreen(onBack: () -> Unit) {
    var speed by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var distance by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Speed/Distance Calculator",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        TextField(
            value = speed,
            onValueChange = {
                speed = it
                if (speed.isNotEmpty() && time.isNotEmpty()) {
                    distance = ((speed.toDoubleOrNull() ?: 0.0) * (time.toDoubleOrNull() ?: 0.0)).toString()
                } else if (speed.isNotEmpty() && distance.isNotEmpty()) {
                    time = ((distance.toDoubleOrNull() ?: 0.0) / (speed.toDoubleOrNull() ?: 1.0)).toString()
                }
            },
            label = { Text("Speed (knots)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = time,
            onValueChange = {
                time = it
                if (speed.isNotEmpty() && time.isNotEmpty()) {
                    distance = ((speed.toDoubleOrNull() ?: 0.0) * (time.toDoubleOrNull() ?: 0.0)).toString()
                } else if (time.isNotEmpty() && distance.isNotEmpty()) {
                    speed = ((distance.toDoubleOrNull() ?: 0.0) / (time.toDoubleOrNull() ?: 1.0)).toString()
                }
            },
            label = { Text("Time (hours)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = distance,
            onValueChange = {
                distance = it
                if (speed.isNotEmpty() && distance.isNotEmpty()) {
                    time = ((distance.toDoubleOrNull() ?: 0.0) / (speed.toDoubleOrNull() ?: 1.0)).toString()
                } else if (time.isNotEmpty() && distance.isNotEmpty()) {
                    speed = ((distance.toDoubleOrNull() ?: 0.0) / (time.toDoubleOrNull() ?: 1.0)).toString()
                }
            },
            label = { Text("Distance (nautical miles)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onBack) {
            Text("Back")
        }
    }
}

@Composable
fun LogsScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Trip Logs",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(text = "Logs feature coming soon...")

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onBack) {
            Text("Back")
        }
    }
}