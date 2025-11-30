package com.example.trekapp1.views

import android.Manifest
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.trekapp1.controllers.TrackingController
import com.example.trekapp1.ui.theme.CoralOrange
import com.example.trekapp1.ui.theme.CoralPink
import com.example.trekapp1.ui.theme.DarkBackground
import com.example.trekapp1.views.components.CompactStatCard
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import kotlinx.coroutines.launch

@Composable
fun MapTrackingView(
    trackingController: TrackingController,
    onEndSession: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var hasLocationPermission by remember { mutableStateOf(false) }
    val sessionStats = trackingController.sessionStats

    var mapView by remember { mutableStateOf<MapView?>(null) }
    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }
    val currentLocation = trackingController.currentLocation
    val routePoints = trackingController.routePoints
    var userMarker by remember { mutableStateOf<com.google.android.gms.maps.model.Marker?>(null) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (hasLocationPermission) {
            try {
                googleMap?.isMyLocationEnabled = true
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        val userLocation = LatLng(it.latitude, it.longitude)
                        googleMap?.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(userLocation, 17f)
                        )
                    }
                }
                trackingController.startTracking(scope)
            } catch (e: SecurityException) {
                // Handle error
            }
        }
    }

    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // Update camera AND marker when user moves
    LaunchedEffect(currentLocation) {
        currentLocation?.let { location ->
            googleMap?.let { map ->
                // Move camera to follow user
                map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(location, 17f)
                )

                // Update user position marker (blue dot replacement)
                if (userMarker == null) {
                    userMarker = map.addMarker(
                        MarkerOptions()
                            .position(location)
                            .title("You are here")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    )
                } else {
                    userMarker?.position = location
                }
            }
        }
    }

    // Draw route line
    LaunchedEffect(routePoints.size) {
        if (routePoints.size > 1) {
            googleMap?.let { map ->
                // Clear old route
                map.clear()

                // Re-add user marker
                currentLocation?.let { loc ->
                    userMarker = map.addMarker(
                        MarkerOptions()
                            .position(loc)
                            .title("You are here")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    )
                }

                // Draw route
                map.addPolyline(
                    PolylineOptions()
                        .addAll(routePoints)
                        .color(android.graphics.Color.parseColor("#FF6B35"))
                        .width(10f)
                )
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            trackingController.stopTracking()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Stats Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CompactStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Steps",
                    value = sessionStats.steps,
                    icon = Icons.Default.DirectionsRun
                )
                CompactStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Distance",
                    value = sessionStats.distance,
                    icon = Icons.Default.Map
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CompactStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Calories",
                    value = sessionStats.calories,
                    icon = Icons.Default.LocalFireDepartment
                )
                CompactStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Time",
                    value = sessionStats.time,
                    icon = Icons.Default.Timer
                )
            }
        }

        // Map View
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
        ) {
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        onCreate(Bundle())
                        onResume()
                        mapView = this

                        getMapAsync { map ->
                            googleMap = map

                            map.uiSettings.apply {
                                isZoomControlsEnabled = true
                                isCompassEnabled = true
                                isMyLocationButtonEnabled = true
                            }

                            if (hasLocationPermission) {
                                try {
                                    map.isMyLocationEnabled = true

                                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                                        location?.let {
                                            val userLocation = LatLng(it.latitude, it.longitude)
                                            map.animateCamera(
                                                CameraUpdateFactory.newLatLngZoom(userLocation, 17f)
                                            )
                                            // Add initial marker
                                            userMarker = map.addMarker(
                                                MarkerOptions()
                                                    .position(userLocation)
                                                    .title("You are here")
                                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                                            )
                                        }
                                    }
                                } catch (e: SecurityException) {
                                    // Handle error
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (currentLocation == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(color = CoralOrange)
                        Text(
                            "Getting your location...",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // End Session Button
        Button(
            onClick = {
                trackingController.stopTracking()
                onEndSession()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent
            ),
            contentPadding = PaddingValues()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(CoralOrange, CoralPink)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        "End Session",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}