package com.example.trekapp1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.trekapp1.controllers.*
import com.example.trekapp1.ui.theme.CardBackground
import com.example.trekapp1.ui.theme.DarkBackground
import com.example.trekapp1.ui.theme.RunningAppTheme
import com.example.trekapp1.utils.Screen
import com.example.trekapp1.views.*
import com.example.trekapp1.views.components.NavigationDrawerContent
import kotlinx.coroutines.launch

/**
 * Main activity for the Trek running app.
 * Manages controllers and navigation between screens.
 */
class MainActivity : ComponentActivity() {
    /** Manager for accessing HealthConnect API. */
    private lateinit var healthConnectManager: HealthConnectManager

    /** Controller for dashboard statistics. */
    private lateinit var dashboardController: DashboardController

    /** Controller for activity records. */
    private lateinit var activityController: ActivityController

    /** Controller for avatar shop. */
    private lateinit var avatarController: AvatarController

    /** Controller for tracking sessions. */
    private lateinit var trackingController: TrackingController

    /**
     * Called when the activity is created.
     * Initializes managers, controllers, and sets up the UI.
     *
     * @param savedInstanceState Saved state from previous instance.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize managers
        healthConnectManager = HealthConnectManager(this)

        // Initialize controllers
        dashboardController = DashboardController(healthConnectManager)
        activityController = ActivityController()
        avatarController = AvatarController()
        trackingController = TrackingController()

        setContent {
            RunningAppTheme {
                MainScreen(
                    dashboardController = dashboardController,
                    activityController = activityController,
                    avatarController = avatarController,
                    trackingController = trackingController
                )
            }
        }
    }
}

/**
 * Main screen composable managing navigation and screen content.
 * Handles drawer navigation and switching between different views.
 *
 * @param dashboardController Controller for dashboard statistics.
 * @param activityController Controller for activity records.
 * @param avatarController Controller for avatar shop.
 * @param trackingController Controller for tracking sessions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    dashboardController: DashboardController,
    activityController: ActivityController,
    avatarController: AvatarController,
    trackingController: TrackingController
) {
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
                        title = {
                            Text(
                                if (isTrackingSession) "Trek in Progress"
                                else selectedScreen.title
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                if (isTrackingSession) {
                                    isTrackingSession = false
                                } else {
                                    scope.launch { drawerState.open() }
                                }
                            }) {
                                Icon(
                                    if (isTrackingSession) Icons.Default.ArrowBack
                                    else Icons.Default.Menu,
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
                        // Show tracking view when session is active
                        MapTrackingView(
                            trackingController = trackingController,
                            onEndSession = { isTrackingSession = false }
                        )
                    } else {
                        // Show selected screen from navigation
                        when (selectedScreen) {
                            Screen.Dashboard -> DashboardView(
                                dashboardController = dashboardController,
                                activityController = activityController,
                                onStartRun = { isTrackingSession = true }
                            )
                            Screen.Activities -> ActivitiesView(
                                activityController = activityController
                            )
                            Screen.Avatars -> AvatarsView(
                                avatarController = avatarController
                            )
                        }
                    }
                }
            }
        }
    )
}