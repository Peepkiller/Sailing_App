package com.example.sailingapp

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.sailingapp.PlacesHelper.getPlaceDetails
import com.example.sailingapp.ui.theme.SailingAppTheme
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class MainActivity : ComponentActivity() {

    private lateinit var locationHelper: LocationHelper
    private lateinit var placesClient: PlacesClient
    private var userLatitude by mutableDoubleStateOf(0.0)
    private var userLongitude  by mutableDoubleStateOf(0.0)
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseDatabase.getInstance().setPersistenceEnabled(true)

        // Initialize PlacesClient
        placesClient = PlacesHelper.initializePlaces(this)

        // Initialize LocationHelper
        locationHelper = LocationHelper(this)

        // Initialize the permission request launcher
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                fetchLocation()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }

        checkLocationPermission()

        // Set up UI
        setContent {
            SailingAppTheme {
                MainScreen(
                    userLatitude = userLatitude,
                    userLongitude = userLongitude,
                    placesClient = placesClient,
                )
            }
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)

        } else {
            fetchLocation()
        }
    }

    private fun fetchLocation() {
        locationHelper.fetchLocation { latitude, longitude ->
            userLatitude = latitude
            userLongitude = longitude
        }
    }
}

    @Composable
    fun MainScreen(userLatitude: Double, userLongitude: Double, placesClient: PlacesClient) {
        var selectedTab by remember { mutableIntStateOf(0) } // State for tab selection
        var searchQuery by remember { mutableStateOf("") } // State for search query
        var moreScreenSubpage by remember { mutableStateOf("") } // Tracks subpage navigation in MoreScreen
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(LatLng(userLatitude, userLongitude), 15f) // Initialize camera position
        }

        Scaffold(
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
                    onSearchQueryChanged = { newQuery -> searchQuery = newQuery },
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
    onSearchQueryChanged: (String) -> Unit,
    placesClient: PlacesClient,
    cameraPositionState: CameraPositionState
) {
    var lat by remember { mutableDoubleStateOf(userLatitude) }
    var lng by remember { mutableDoubleStateOf(userLongitude) }
    var isLocationInitialized by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val locationHelper = remember { LocationHelper(context as Activity) }
    val recentSearches by remember { mutableStateOf(RecentSearchesHelper.getSearchHistory(context)) }
    var showRecentSearches by remember { mutableStateOf(searchQuery.isEmpty()) }
    val focusRequester = remember { FocusRequester() }
    var isSearchFocused by remember { mutableStateOf(false) }
    var selectedMarker by remember { mutableStateOf<LatLng?>(null) }

    val markerState = remember { mutableStateOf(MarkerState(LatLng(lat, lng))) }
    var searchedLatLng by remember { mutableStateOf<LatLng?>(null) }
    var predictions by remember { mutableStateOf<List<AutocompletePrediction>>(emptyList()) }
    var showPredictions by remember { mutableStateOf(false) }
    var route by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    // Initialize location on app launch
    LaunchedEffect(Unit) {
        locationHelper.fetchLocation { latitude, longitude ->
            lat = latitude
            lng = longitude
            cameraPositionState.position = CameraPosition.fromLatLngZoom(LatLng(lat, lng), 15f)
            markerState.value = MarkerState(LatLng(lat, lng))
            isLocationInitialized = true
        }
    }

    LaunchedEffect(cameraPositionState.isMoving) {
        if (cameraPositionState.isMoving) {
            showRecentSearches = false
            isSearchFocused = false
            showPredictions = false
        }
    }

    LaunchedEffect(isSearchFocused) {
        if (isSearchFocused && searchQuery.isEmpty()) {
            showRecentSearches = true
        }
    }

    LaunchedEffect(searchedLatLng) {
        searchedLatLng?.let { destination ->
            coroutineScope.launch {
                val fetchedRoute = getDirections(markerState.value.position, destination)
                withContext(Dispatchers.Main) {
                    route = fetchedRoute
                    FirebaseLogHelper.saveLog(
                        title = destination.toString(),
                        notes = "Automatically saved trip",
                        routePoints = fetchedRoute,
                        startLocation = markerState.value.position.toString(),
                        endLocation = destination.toString(),
                        onSuccess = { Log.d("Firebase", "Trip saved successfully") },
                        onFailure = { Log.e("Firebase", "Error saving trip: ${it.message}") }
                    )
                }
            }
        }
    }

    selectedMarker?.let { location ->
        Box(
            modifier = Modifier
                .padding(bottom = 100.dp)
                .background(Color.White, shape = RoundedCornerShape(12.dp))
                .padding(8.dp)
        ) {
            Button(
                onClick = {
                    coroutineScope.launch {
                        val fetchedRoute = getDirections(markerState.value.position, location)
                        withContext(Dispatchers.Main) {
                            route = fetchedRoute
                            Log.d("GpsLogsScreen", "Route button clicked - ${route.size} points")
                        }
                    }
                }
            ) {
                Text("Get Directions")
            }
        }
    }


    Box(modifier = modifier.fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        isSearchFocused = false
                        showRecentSearches = false
                    }
                )
            }
    ) {
        // Google Map setup
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(
                compassEnabled = true,
                myLocationButtonEnabled = false
            ),
            onMapClick = { selectedMarker = null }
        ) {
            // Marker for user's current location
            if (isLocationInitialized) {
                Marker(
                    state = markerState.value,
                    title = "You are here"
                )
            }

            // Marker for searched location
            searchedLatLng?.let { location ->
                Marker(
                    state = MarkerState(location),
                    title = "Searched Location",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE),
                    onClick = {
                        selectedMarker = location
                        true
                    }
                )
            }

            if (route.isNotEmpty()) {
                Polyline(
                    points = route,
                    color = Color.Blue,
                    width = 8f
                )
            }
        }

        selectedMarker?.let { location ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp)
                    .background(Color.White, shape = RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            route = getDirections(markerState.value.position, location) // Fetch route
                        }
                    }
                ) {
                    Text("Get Directions")
                }
            }
        }

        // Floating Action Button to fetch and update location
        FloatingActionButton(
            onClick = {
                locationHelper.fetchLocation { latitude, longitude ->
                    lat = latitude
                    lng = longitude
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(LatLng(lat, lng), 15f)
                    markerState.value = MarkerState(LatLng(lat, lng))
                }
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp,  top = 90.dp)
        ) {
            Icon(imageVector = Icons.Filled.LocationOn, contentDescription = "Find me")
        }

        // Search bar logic for places
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Color.White, shape = RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            TextField(
                value = searchQuery,
                onValueChange = {
                    onSearchQueryChanged(it)
                    showRecentSearches = it.isEmpty() && isSearchFocused
                    if (it.isNotEmpty()) {
                        // Fetch place predictions
                        val request = FindAutocompletePredictionsRequest.builder()
                            .setQuery(it)
                            .build()

                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val response =
                                    placesClient.findAutocompletePredictions(request).await()
                                withContext(Dispatchers.Main) {
                                    predictions = response.autocompletePredictions
                                    showPredictions = predictions.isNotEmpty()
                                }
                            } catch (e: Exception) {
                                println("Error fetching places: ${e.message}")
                            }
                        }
                    } else {
                        showPredictions = false
                    }
                },
                placeholder = { Text("Search places...") },
                singleLine = true,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        isSearchFocused = focusState.isFocused
                        if (isSearchFocused && searchQuery.isEmpty()) {
                            showRecentSearches = true
                        }
                    }
            )

            // Show recent searches when search box is empty
            if (showRecentSearches && isSearchFocused && recentSearches.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .heightIn(max = 150.dp)
                ) {
                    items(recentSearches) { recent ->
                        Text(
                            text = recent,
                            color = Color.Black,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSearchQueryChanged(recent)
                                    showRecentSearches = false
                                    isSearchFocused = false
                                    getPlaceDetails(recent, placesClient) { latLng ->
                                        if (latLng != null) {
                                            searchedLatLng = latLng
                                            cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 15f)
                                        }
                                    }
                                }
                                .padding(8.dp)
                        )
                    }
                }
            }


            // Show predictions list when user types
            if (showPredictions) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .heightIn(max = 200.dp)
                ) {
                    items(predictions) { prediction ->
                        Text(
                            text = prediction.getPrimaryText(null).toString(),
                            color = Color.Black,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showPredictions = false
                                    getPlaceDetails(prediction.placeId, placesClient) { latLng ->
                                        if (latLng != null) {
                                            searchedLatLng = latLng
                                            RecentSearchesHelper.saveSearch(context, prediction.getPrimaryText(null).toString()) // Save recent search
                                            cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 15f)
                                        }
                                    }
                                }
                                .padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun WeatherScreen(modifier: Modifier = Modifier, latitude: Double, longitude: Double)  {
    var weatherData by remember { mutableStateOf<String?>(null) }
    var tideData by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val database = FirebaseDatabase.getInstance()
    val safeLatLng = "${latitude.toString().replace(".", "_dot_").replace("-", "_neg_")}_${longitude.toString().replace(".", "_dot_").replace("-", "_neg_")}"
    val weatherRootRef = database.getReference("weather") // Root reference
    var weatherRef by remember { mutableStateOf(database.getReference("weather")) } // Mutable reference

    val stationId = "8720218"
    val tideRef = database.getReference("tide").child(stationId)

    // Ensure offline sync
    tideRef.keepSynced(true)

    val weatherLoaded by remember { mutableStateOf(false) }
    var tideLoaded by remember { mutableStateOf(false) }

    fun fetchData() {
        coroutineScope.launch {
            isLoading = true
            try {
                if (weatherData != null && weatherData != "No weather data available. Connect to the internet to refresh.") {
                    Log.d("WeatherScreen", "Offline mode: Keeping cached weather data")
                    isLoading = false
                    return@launch // Exit early if offline & already have cached data
                }
                try {
                    val weatherUrl =
                        "https://api.open-meteo.com/v1/forecast?latitude=52.52&longitude=13.41&current=temperature_2m,precipitation,wind_speed_10m&hourly=temperature_2m,precipitation_probability,rain,visibility,wind_speed_10m&daily=temperature_2m_max,temperature_2m_min,sunrise&temperature_unit=fahrenheit&wind_speed_unit=kn&precipitation_unit=inch&timezone=GMT"

                    try {
                        val weatherResponse = fetchWeatherDataWithGson(weatherUrl)
                        val formattedWeatherData = """    
                Temperature: ${weatherResponse.current.temperature_2m} ${weatherResponse.current_units.temperature_2m}
                Wind Speed: ${weatherResponse.current.wind_speed_10m} ${weatherResponse.current_units.wind_speed_10m}
                Precipitation: ${weatherResponse.current.precipitation} ${weatherResponse.current_units.precipitation}
                Max Temp: ${weatherResponse.daily.temperature_2m_max[0]} ${weatherResponse.daily_units.temperature_2m_max}
                Min Temp: ${weatherResponse.daily.temperature_2m_min[0]} ${weatherResponse.daily_units.temperature_2m_min}
                Sunrise: ${weatherResponse.daily.sunrise[0]}
                """.trimIndent()
                        weatherData = formattedWeatherData
                        weatherRef.setValue(formattedWeatherData)

                    } catch (e: Exception) {
                        Log.e("WeatherScreen", "Weather API Fetch Error: ${e.message}", e)
                        weatherData = "Error fetching weather data"
                        weatherRef.setValue("Error fetching weather data")
                    }

                    // Fetch tide data
                    val tideResponse = TideService.fetchTideData(stationId)
                    if (tideResponse != null) {
                        tideData =
                            tideResponse.data?.joinToString("\n") { "${it.time}: ${it.height} ft" }
                                ?: "No tide data available"

                        if (tideResponse.data != null) {
                            tideRef.setValue(tideResponse)
                        }

                    } else {
                        Log.e("WeatherScreen", "Failed to fetch tide data")
                    }

                } catch (e: Exception) {
                    errorMessage = "Error fetching data: ${e.localizedMessage}"
                    Log.e("WeatherScreen", "API Fetch Error: ${e.message}", e)
                } finally {
                    isLoading = false
                }
            } finally {
                isLoading = false
            }
        }
    }

    // Load Firebase data first, then fetch API data if needed
    LaunchedEffect(Unit) {
        isLoading = true

        findClosestWeatherKey(weatherRootRef, safeLatLng) { closestKey ->
            if (closestKey != null) {
                weatherRef = database.getReference("weather/$closestKey")
                Log.d("WeatherScreen", "Using closest weather key: $closestKey")
            } else {
                Log.e("WeatherScreen", "No close weather key found, using default path")
                weatherRef = database.getReference("weather/$safeLatLng") // Keep fallback
            }

            fun checkIfDataIsMissing() {
                if (weatherLoaded && tideLoaded) {
                    if (weatherData == "No weather data available" || tideData == "No tide data available") {
                        fetchData()
                    } else {
                        isLoading = false
                    }
                }
            }

            val weatherListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    weatherData = if (snapshot.exists() && snapshot.value != null) {
                        snapshot.getValue(String::class.java) ?: "No cached weather data"
                    } else {
                        "No weather data available. Connect to the internet to refresh."
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    errorMessage = "Offline mode: Using last cached weather data."
                }
            }

            val tideListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists() && snapshot.hasChild("data")) {
                        val tideDataList = snapshot.child("data").children.mapNotNull {
                            try {
                                it.getValue(TideDataPoint::class.java)
                            } catch (e: Exception) {
                                Log.e("WeatherScreen", "Error parsing tide data: ${e.message}")
                                null
                            }
                        }
                        tideData = if (tideDataList.isNotEmpty()) {
                            tideDataList.joinToString("\n") { "${it.time}: ${it.height} ft" }
                        } else {
                            "No tide data available"
                        }
                    } else {
                        Log.e("WeatherScreen", "No tide data found in Firebase")
                        tideData = "No tide data available"
                    }

                    tideLoaded = true
                    checkIfDataIsMissing()
                }

                override fun onCancelled(error: DatabaseError) {
                    errorMessage = "Firebase tide error: ${error.message}"
                    Log.e("WeatherScreen", "Firebase Tide Error: ${error.message}")
                    isLoading = false
                }
            }

            // Attach listeners
            weatherRef.addListenerForSingleValueEvent(weatherListener)
            tideRef.addListenerForSingleValueEvent(tideListener)
        }
    }

    // Refresh Button Effect
    LaunchedEffect(weatherData, tideData) {
        isLoading = false
    }

    // UI rendering
    Box(modifier = modifier
        .fillMaxSize()
        .padding(16.dp)
    ) {
        when {
            isLoading -> Text("Loading weather and tide data...", Modifier.align(Alignment.Center))
            errorMessage != null -> Text("Error: $errorMessage", Modifier.align(Alignment.Center))
            else -> {
                Column {
                    // Refresh Button
                    Button(
                        onClick = { if (!isLoading) fetchData() },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Text(if (isLoading) "Loading..." else "Refresh")
                    }

                    // Weather Data
                    Text("Weather Data:", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 8.dp))
                    Text(weatherData ?: "No weather data available")

                    Spacer(modifier = Modifier.height(16.dp))

                    // Tide Data
                    Text("Tide Data:", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
                    Text(tideData ?: "No tide data available")
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

        Box(modifier = Modifier
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
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var tripLogs by remember { mutableStateOf<List<TripLog>>(emptyList()) }
    var isSaving by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Load logs from Firebase
    LaunchedEffect(Unit) {
        FirebaseLogHelper.getLogs(
            onLogsFetched = { logs -> tripLogs = logs },
            onFailure = { error -> Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show() }
        )
    }

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

        // Title Input
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Notes Input
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes") },
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Save Button
        Button(
            onClick = {
                if (title.isNotEmpty() && notes.isNotEmpty()) {
                    isSaving = true
                    FirebaseLogHelper.saveLog(
                        title = title,
                        notes = notes,
                        routePoints = emptyList(), // Can add routePoints later when saving trip directions
                        startLocation = "Start Point",
                        endLocation = "End Point",
                        onSuccess = {
                            Toast.makeText(context, "Log saved!", Toast.LENGTH_SHORT).show()
                            title = ""
                            notes = ""
                            isSaving = false
                        },
                        onFailure = { error ->
                            Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                            isSaving = false
                        }
                    )
                } else {
                    Toast.makeText(context, "Title and Notes are required!", Toast.LENGTH_SHORT).show()
                }
            },
            enabled = !isSaving
        ) {
            Text(if (isSaving) "Saving..." else "Save Log")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Display Logs List
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(tripLogs) { log ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(text = log.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(text = log.notes, fontSize = 14.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        Text(text = "From: ${log.startLocation} To: ${log.endLocation}", fontSize = 12.sp)
                        Text(text = "Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(
                            Date(log.timestamp)
                        )}", fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Back Button
        Button(onClick = onBack) {
            Text("Back")
        }
    }
}

