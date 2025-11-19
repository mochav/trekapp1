package com.example.trekapp1

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RunningAppTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun RunningAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = CoralPink,
            secondary = CoralOrange,
            background = DarkBackground,
            surface = CardBackground
        ),
        content = content
    )
}

// Color Scheme
val CoralOrange = Color(0xFFFFA07A)
val CoralPink = Color(0xFFFF6B9D)
val DarkBackground = Color(0xFF1A1A1A)
val CardBackground = Color(0xFF2A2A2A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedScreen by remember { mutableStateOf(Screen.Dashboard) }
    var isTrackingSession by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            NavigationDrawerContent(
                selectedScreen = selectedScreen,
                onScreenSelected = { screen ->
                    selectedScreen = screen
                    isTrackingSession = false
                    scope.launch { drawerState.close() }
                }
            )
        },
        content = {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(if (isTrackingSession) "Trek in Progress" else selectedScreen.title) },
                        navigationIcon = {
                            IconButton(onClick = {
                                if (isTrackingSession) {
                                    isTrackingSession = false
                                } else {
                                    scope.launch { drawerState.open() }
                                }
                            }) {
                                Icon(
                                    if (isTrackingSession) Icons.Default.ArrowBack else Icons.Default.Menu,
                                    if (isTrackingSession) "Back" else "Menu"
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = CardBackground
                        )
                    )
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(DarkBackground)
                ) {
                    if (isTrackingSession) {
                        MapTrackingView(
                            onEndSession = { isTrackingSession = false }
                        )
                    } else {
                        when (selectedScreen) {
                            Screen.Dashboard -> DashboardView(
                                onStartRun = { isTrackingSession = true }
                            )
                            Screen.Activities -> ActivitiesView()
                            Screen.Avatars -> AvatarsView()
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun MapTrackingView(onEndSession: () -> Unit) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var hasLocationPermission by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
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
            // Stats Row 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CompactStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Steps",
                    value = "3,542",
                    icon = Icons.Default.DirectionsRun
                )
                CompactStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Distance",
                    value = "2.8 km",
                    icon = Icons.Default.Map
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stats Row 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CompactStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Calories",
                    value = "245",
                    icon = Icons.Default.LocalFireDepartment
                )
                CompactStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Time",
                    value = "28:15",
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
                            googleMap.uiSettings.apply {
                                isZoomControlsEnabled = true
                                isCompassEnabled = true
                                isMyLocationButtonEnabled = true
                            }

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
            onClick = onEndSession,
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

@Composable
fun CompactStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = CardBackground
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Icon(
                icon,
                contentDescription = null,
                tint = CoralPink,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun NavigationDrawerContent(
    selectedScreen: Screen,
    onScreenSelected: (Screen) -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = CardBackground
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(CoralOrange, CoralPink)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.DirectionsRun,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Color.White
                )
            }
        }

        Text(
            "Running App",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(32.dp))

        Screen.values().forEach { screen ->
            NavigationDrawerItem(
                icon = { Icon(screen.icon, contentDescription = null) },
                label = { Text(screen.title) },
                selected = selectedScreen == screen,
                onClick = { onScreenSelected(screen) },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = CoralPink.copy(alpha = 0.2f),
                    selectedIconColor = CoralPink,
                    selectedTextColor = CoralPink
                )
            )
        }
    }
}

@Composable
fun DashboardView(onStartRun: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Distance",
                value = "24.5",
                unit = "km",
                icon = Icons.Default.DirectionsRun
            )
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Time",
                value = "2:15",
                unit = "hrs",
                icon = Icons.Default.Timer
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Calories",
                value = "1,245",
                unit = "kcal",
                icon = Icons.Default.LocalFireDepartment
            )
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Pace",
                value = "5:32",
                unit = "min/km",
                icon = Icons.Default.Speed
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onStartRun,
            modifier = Modifier
                .fillMaxWidth()
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
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        "Start Run",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Recent Activities",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(12.dp))

        ActivityCard(
            date = "Today, 8:30 AM",
            distance = "5.2 km",
            duration = "28:15",
            pace = "5:25 min/km"
        )

        Spacer(modifier = Modifier.height(8.dp))

        ActivityCard(
            date = "Yesterday, 7:00 AM",
            distance = "7.8 km",
            duration = "42:30",
            pace = "5:27 min/km"
        )
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    unit: String,
    icon: ImageVector
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = CardBackground
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Icon(
                    icon,
                    contentDescription = null,
                    tint = CoralPink,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    unit,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}

@Composable
fun ActivityCard(
    date: String,
    distance: String,
    duration: String,
    pace: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = CardBackground
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    date,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    distance,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CoralPink
                )
            }
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    duration,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    pace,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun ActivitiesView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "All Activities",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(16.dp))

        repeat(5) { index ->
            ActivityCard(
                date = "${5 - index} days ago",
                distance = "${(5 + index).toFloat()} km",
                duration = "${25 + index * 3}:${15 + index * 2}",
                pace = "5:${20 + index} min/km"
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun AvatarsView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Avatar Shop - Coming Soon!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

enum class Screen(val title: String, val icon: ImageVector) {
    Dashboard("Dashboard", Icons.Default.Dashboard),
    Activities("Activities", Icons.Default.DirectionsRun),
    Avatars("Avatars", Icons.Default.Person)
}