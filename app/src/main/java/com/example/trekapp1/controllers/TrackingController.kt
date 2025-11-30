package com.example.trekapp1.controllers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.core.content.ContextCompat
import com.example.trekapp1.models.TrackingSessionStats
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TrackingController(private val context: Context) {  // ADD context parameter

    var sessionStats by mutableStateOf(TrackingSessionStats())
        private set

    var isTracking by mutableStateOf(false)
        private set

    // ========== ADD THESE NEW PROPERTIES ==========
    // Current user location for map
    var currentLocation by mutableStateOf<LatLng?>(null)
        private set

    // Route points for drawing path on map
    val routePoints = mutableStateListOf<LatLng>()

    // Google Location Services
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        1000L // Update every 1 second
    ).build()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let { location ->
                val latLng = LatLng(location.latitude, location.longitude)
                currentLocation = latLng
                routePoints.add(latLng)
            }
        }
    }
    // ========== END NEW PROPERTIES ==========

    private var trackingJob: Job? = null
    private var elapsedSeconds = 0

    fun startTracking(scope: CoroutineScope) {
        if (isTracking) return

        isTracking = true
        elapsedSeconds = 0
        sessionStats = TrackingSessionStats()

        // ========== ADD THIS: Start GPS updates ==========
        routePoints.clear()
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
            } catch (e: SecurityException) {
                // Permission error
            }
        }
        // ========== END GPS updates ==========

        trackingJob = scope.launch {
            while (isTracking) {
                delay(1000)
                elapsedSeconds++
                updateStats()
            }
        }
    }

    fun stopTracking() {
        isTracking = false
        trackingJob?.cancel()
        trackingJob = null

        // ========== ADD THIS: Stop GPS updates ==========
        fusedLocationClient.removeLocationUpdates(locationCallback)
        // ========== END ==========
    }

    private fun updateStats() {
        val minutes = elapsedSeconds / 60
        val seconds = elapsedSeconds % 60

        sessionStats = sessionStats.copy(
            steps = (elapsedSeconds * 2).toString(), // Keep this - your classmate will fix
            distance = String.format("%.2f km", elapsedSeconds * 0.001), // Keep this
            calories = (elapsedSeconds / 10).toString(), // Keep this
            time = String.format("%02d:%02d", minutes, seconds)
        )
    }

    fun reset() {
        stopTracking()
        elapsedSeconds = 0
        sessionStats = TrackingSessionStats()
        // ========== ADD THIS ==========
        routePoints.clear()
        currentLocation = null
        // ========== END ==========
    }
}