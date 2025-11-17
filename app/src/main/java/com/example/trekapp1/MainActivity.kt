package com.example.trekapp1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
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
    var isTracking by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            NavigationDrawerContent(
                selectedScreen = selectedScreen,
                onScreenSelected = { screen ->
                    selectedScreen = screen
                    scope.launch { drawerState.close() }
                }
            )
        },
        content = {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(if (isTracking) "Tracking Run" else selectedScreen.title) },
                        navigationIcon = {
                            IconButton(onClick = {
                                if (!isTracking) {
                                    scope.launch { drawerState.open() }
                                }
                            }) {
                                Icon(Icons.Default.Menu, "Menu")
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
                    if (isTracking) {
                        RunTrackingScreen(
                            onStopTracking = { isTracking = false }
                        )
                    } else {
                        when (selectedScreen) {
                            Screen.Dashboard -> DashboardView(onStartRun = { isTracking = true })
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
fun NavigationDrawerContent(
    selectedScreen: Screen,
    onScreenSelected: (Screen) -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = CardBackground
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // App Logo/Header
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
            "Trek",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Navigation Items
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
fun DashboardView(onStartRun: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Stats Cards
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

        // Start Run Button
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

        // Recent Activities Section
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

        // Sample activities
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Choose Your Avatar",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Avatar Grid
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AvatarItem(Modifier.weight(1f), "Runner", Icons.Default.DirectionsRun)
                AvatarItem(Modifier.weight(1f), "Athlete", Icons.Default.FitnessCenter)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AvatarItem(Modifier.weight(1f), "Champion", Icons.Default.EmojiEvents)
                AvatarItem(Modifier.weight(1f), "Explorer", Icons.Default.Explore)
            }
        }
    }
}

@Composable
fun AvatarItem(modifier: Modifier, name: String, icon: ImageVector) {
    Card(
        modifier = modifier.clickable { },
        colors = CardDefaults.cardColors(
            containerColor = CardBackground
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(CoralOrange, CoralPink)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

// Run Tracking Data Class
data class RunData(
    val elapsedTimeSeconds: Int = 0,
    val distanceKm: Float = 0f,
    val caloriesBurned: Int = 0,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false
)

@Composable
fun RunTrackingScreen(onStopTracking: () -> Unit) {
    var runData by remember { mutableStateOf(RunData()) }
    val scope = rememberCoroutineScope()

    // Timer effect
    LaunchedEffect(runData.isRunning, runData.isPaused) {
        if (runData.isRunning && !runData.isPaused) {
            while (true) {
                delay(1000L)
                runData = runData.copy(
                    elapsedTimeSeconds = runData.elapsedTimeSeconds + 1,
                    // Simulate distance increase (adjust based on your needs)
                    // Average running speed: ~10 km/h = 0.00278 km/s
                    distanceKm = runData.distanceKm + 0.00278f,
                    // Simulate calorie burn (adjust based on your needs)
                    // Average: ~60 calories per km
                    caloriesBurned = (runData.distanceKm * 60).toInt()
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Large Timer Display
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = CardBackground
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Time",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    formatTime(runData.elapsedTimeSeconds),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = CoralPink
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RunStatCard(
                modifier = Modifier.weight(1f),
                title = "Distance",
                value = String.format("%.2f", runData.distanceKm),
                unit = "km",
                icon = Icons.Default.DirectionsRun
            )
            RunStatCard(
                modifier = Modifier.weight(1f),
                title = "Calories",
                value = runData.caloriesBurned.toString(),
                unit = "kcal",
                icon = Icons.Default.LocalFireDepartment
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Pace Display
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = CardBackground
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Speed,
                        contentDescription = null,
                        tint = CoralPink,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Current Pace",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                Text(
                    calculatePace(runData.distanceKm, runData.elapsedTimeSeconds),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Control Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Start/Pause Button
            Button(
                onClick = {
                    if (!runData.isRunning) {
                        runData = runData.copy(isRunning = true, isPaused = false)
                    } else {
                        runData = runData.copy(isPaused = !runData.isPaused)
                    }
                },
                modifier = Modifier
                    .weight(1f)
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
                            if (!runData.isRunning || runData.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            if (!runData.isRunning) "Start" else if (runData.isPaused) "Resume" else "Pause",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Stop Button
            Button(
                onClick = {
                    // Here you would save the run data to your database
                    // Example:
                    // scope.launch {
                    //     database.insertRun(
                    //         distance = runData.distanceKm,
                    //         duration = runData.elapsedTimeSeconds,
                    //         calories = runData.caloriesBurned,
                    //         pace = calculatePace(runData.distanceKm, runData.elapsedTimeSeconds)
                    //     )
                    // }
                    onStopTracking()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CardBackground
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = CoralPink
                    )
                    Text(
                        "Finish",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun RunStatCard(
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = CoralPink,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(4.dp))
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

// Helper Functions
fun formatTime(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%02d:%02d", minutes, secs)
    }
}

fun calculatePace(distanceKm: Float, timeSeconds: Int): String {
    if (distanceKm == 0f) return "--:--"
    val paceSecondsPerKm = timeSeconds / distanceKm
    val minutes = (paceSecondsPerKm / 60).toInt()
    val seconds = (paceSecondsPerKm % 60).toInt()
    return String.format("%d:%02d", minutes, seconds)
}

enum class Screen(val title: String, val icon: ImageVector) {
    Dashboard("Dashboard", Icons.Default.Dashboard),
    Activities("Activities", Icons.Default.DirectionsRun),
    Avatars("Avatars", Icons.Default.Person)
}