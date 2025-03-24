package com.example.assignment6_q1

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.assignment6_q1.ui.theme.Assignment6_Q1Theme

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import java.util.Locale

class MainActivity : ComponentActivity() {
// From Lecture 6 Examples
    private lateinit var fusedLocationClient: FusedLocationProviderClient // gets location from Google Play
    private lateinit var locationRequest: LocationRequest // tells the system abt location updates

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

//        From Lecture 6 examples
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).apply {
            setMinUpdateIntervalMillis(5000)
        }.build()

        setContent {
            Assignment6_Q1Theme {
//                Storing location
                var location by remember { mutableStateOf<Location?>(null) }
                val context = LocalContext.current

//                Location callback that receives the location updates
                val locationCallback = remember {
                    object : LocationCallback() {
                        override fun onLocationResult(locationResult: LocationResult) {
                            location = locationResult.lastLocation
                        }
                    }
                }

//                From Lecture 6 Examples
//                Request Permission launch
                val permissionGranted = remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    )
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    permissionGranted.value = isGranted
                    if (isGranted) {
                        try {
                            fusedLocationClient.requestLocationUpdates(
                                locationRequest,
                                locationCallback,
                                null
                            )
                        } catch (e: SecurityException) {
                            Log.e("Location", "Security Exception: ${e.message}")
                        }
                    }
                }

//                When launched, trigger permission request
                LaunchedEffect(Unit) {
                    if (!permissionGranted.value) {
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    } else {
                        try {
                            fusedLocationClient.requestLocationUpdates(
                                locationRequest,
                                locationCallback,
                                null
                            )
                        } catch (e: SecurityException) {
                            Log.e("Location", "Security Exception: ${e.message}")
                        }
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LocationApp(location, Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun LocationApp(location: Location?, modifier: Modifier = Modifier) {
    val context = LocalContext.current // Get the current application context
    val cameraPositionState = rememberCameraPositionState() // For maps: camera position state
    var markers by remember { mutableStateOf(listOf<LatLng>()) } // Stores markers (the initial marker is from user's location and the rest will be from map clicks)
    var addressText by remember { mutableStateOf("Waiting for location...") } // Store address for reverse geocoding

    // When location updates, move the camera and show the address
    LaunchedEffect(location) {
        location?.let {
            val latLng = LatLng(it.latitude, it.longitude)

//            Moves the camera to the user's position
            cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 15f)

//            Reverse Geocode - From Lecture 6 Examples
            val geocoder = Geocoder(context, Locale.getDefault())
            try { // Get the first address from the result list
                val addressList = geocoder.getFromLocation(it.latitude, it.longitude, 1)
                addressText = addressList?.firstOrNull()?.getAddressLine(0) ?: "No address found"
            } catch (e: Exception) {
                addressText = "Address error: ${e.message}"
                e.printStackTrace()
            }

//            Add initial user location marker if not already
            if (markers.isEmpty()) {
                markers = listOf(latLng)
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
//       Show address text
        Text(text = addressText,
            modifier = Modifier.fillMaxWidth().padding(8.dp))

//      Show Google Map
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            onMapClick = { tappedLoc ->
                markers = markers + tappedLoc // adds new marker when user taps the map
            }
        ) {
//            Marker Icon
            val markerIcon: BitmapDescriptor = remember {
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
            }

//            Display all markers
            markers.forEach { point ->
                Marker(
                    state = MarkerState(position = point),
                    title = "Marker",
                    icon = markerIcon,
                    snippet = "Lat: ${point.latitude}, Lng: ${point.longitude}"
                )
            }
        }
    }


}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Assignment6_Q1Theme {
        LocationApp(null)
    }
}