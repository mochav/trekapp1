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
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.launch

/**
 * Map tracking view for active running/walking sessions.
 * Displays real-time statistics and a map showing the user's location and route.
 *
 * @param trackingController Controller managing the tracking session.
 * @param onEndSession Callback when user ends the tracking session.
 */
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

    /**
     * Location permission launcher for requesting GPS access.
     */
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    // Request location permissions and start tracking when view appears
    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
        trackingController.startTracking(scope)
    }

    // Stop tracking when view is disposed
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
        // Stats Section at Top
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Stats Row 1: Steps and Distance
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

            // Stats Row 2: Calories and Time
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

        // Map View - Takes remaining space
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
                        getMapAsync { googleMap ->
                            // Configure map UI settings
                            googleMap.uiSettings.apply {
                                isZoomControlsEnabled = true
                                isCompassEnabled = true
                                isMyLocationButtonEnabled = true
                            }

                            // Enable location tracking if permission granted
                            if (hasLocationPermission) {
                                try {
                                    googleMap.isMyLocationEnabled = true
                                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                                        location?.let {
                                            val userLocation = LatLng(it.latitude, it.longitude)
                                            googleMap.addMarker(
                                                MarkerOptions()
                                                    .position(userLocation)
                                                    .title("Trek Started")
                                            )
                                            googleMap.animateCamera(
                                                CameraUpdateFactory.newLatLngZoom(userLocation, 15f)
                                            )
                                        }
                                    }
                                } catch (e: SecurityException) {
                                    // Handle permission error
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // End Session Button at Bottom
        Button(
            onClick = {
                scope.launch {
                    trackingController.endSessionAndSave()
                    onEndSession()
                }
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