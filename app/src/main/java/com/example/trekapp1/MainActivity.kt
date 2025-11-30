package com.example.trekapp1

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import com.example.trekapp1.views.LoginScreen
import com.example.trekapp1.views.components.SelectedAvatarDisplay
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.trekapp1.controllers.*
import com.example.trekapp1.localDatabase.LocalDatabase
import com.example.trekapp1.localDatabase.SyncManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectClient.Companion.SDK_AVAILABLE
import androidx.health.connect.client.HealthConnectClient.Companion.SDK_UNAVAILABLE
import androidx.health.connect.client.HealthConnectClient.Companion.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.lifecycleScope
import com.example.trekapp1.controllers.ActivityController
import com.example.trekapp1.controllers.AvatarController
import com.example.trekapp1.controllers.DashboardController
import com.example.trekapp1.controllers.TrackingController
import com.example.trekapp1.sensors.StepSensorManager
import com.example.trekapp1.ui.theme.CardBackground
import com.example.trekapp1.ui.theme.DarkBackground
import com.example.trekapp1.ui.theme.RunningAppTheme
import com.example.trekapp1.utils.Screen
import com.example.trekapp1.views.ActivitiesView
import com.example.trekapp1.views.AvatarsView
import com.example.trekapp1.views.DashboardView
import com.example.trekapp1.views.MapTrackingView
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

    /** Sensor for step live data. */
    private lateinit var stepSensor: StepSensorManager

    /** Local database instance. */
    private lateinit var database: LocalDatabase

    /** Launcher to show HealthConnect Permissions UI. */
    private val requestPermissions =
        registerForActivityResult(
            PermissionController.createRequestPermissionResultContract()
        ) { grantedPermissions ->
            // optional: handle grantedPermissions
        }

    private fun ensureHealthConnectAvailable(): Boolean {
        val availability = HealthConnectClient.getSdkStatus(this)
        return when (availability) {
            SDK_AVAILABLE -> true
            SDK_UNAVAILABLE,
            SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                // can send user to Play Store
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("market://details?id=com.google.android.apps.healthdata")
                    setPackage("com.android.vending")
                }
                startActivity(intent)
                false
            }
            else -> false
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1001) {
            val index = permissions.indexOf(Manifest.permission.ACTIVITY_RECOGNITION)
            if (index >= 0 && grantResults.getOrNull(index) == PackageManager.PERMISSION_GRANTED) {
                // Start sensor and then move on to Health Connect
                stepSensor.start()
                checkHealthConnectPermissions()
            }
        }
    }

    private fun checkHealthConnectPermissions(){
        //Check and request HealthConnect permissions
        lifecycleScope.launch {
            val healthConnectClient = (HealthConnectClient.getOrCreate(this@MainActivity))
            val required = (healthConnectManager.permissions)
            val granted = healthConnectClient.permissionController.getGrantedPermissions()
            if (!granted.containsAll(required)) { //ask for permissions
                requestPermissions.launch(required) //HealthConnect UI
            } else {//permissions granted
                dashboardController.loadStats()
            }
        }
    }

    /**
     * Called when the activity is created.
     * Initializes managers, controllers, and sets up the UI.
     *
     * @param savedInstanceState Saved state from previous instance.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activityController = ActivityController(
            activityDao = LocalDatabase.getInstance(this).activityDao(),
            coroutineScope = lifecycleScope
        )

        // Syncing Firestore to Local Database
        TrekFirebase.getCurrentUserId()?.let { uid ->
            SyncManager.startUserSync(uid)
        }

        if (!ensureHealthConnectAvailable()) { return }//if HealthConnect not available, won't be able to get health data, leave

        // Initialize database
        database = LocalDatabase.getInstance(applicationContext)

        // Initialize managers
        healthConnectManager = HealthConnectManager(this)

        // Initialize activity controller with database
        activityController = ActivityController(
            activityDao = database.activityDao(),
            coroutineScope = lifecycleScope
        )

        stepSensor = StepSensorManager(this){ stepsSinceRun ->
            trackingController.updateStepsFromSensor(stepsSinceRun)
        }//forward sensor updates to tracking controller

        // Initialize controllers
        dashboardController = DashboardController(
            healthConnectManager = healthConnectManager,
            activityController = activityController  // Pass activity controller
        )
        avatarController = AvatarController()

        // FIXED: Pass both context and healthConnectManager
        trackingController = TrackingController(
            context = applicationContext,
            healthConnectManager = healthConnectManager
        )


        // Request ACTIVITY_RECOGNITION (physical activity) permission, then start sensor and HealthConnect permissions UI
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {//older versions need runtime permission
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                    1001
                )
            } else {
                stepSensor.start()
                checkHealthConnectPermissions()
            }
        } else { // Older Android – no runtime permission needed
            stepSensor.start()
            checkHealthConnectPermissions()
        }

        // In your MainActivity.kt, update the login check section to:

        setContent {
            RunningAppTheme {
                var isLoggedIn by remember { mutableStateOf(false) }

                if (isLoggedIn) {
                    MainScreen(
                        dashboardController = dashboardController,
                        activityController = activityController,
                        avatarController = avatarController,
                        trackingController = trackingController
                    )
                } else {
                    LoginScreen(onLoginSuccess = {
                        isLoggedIn = true
                    })
                }
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
                        actions = {
                            SelectedAvatarDisplay(avatarController)
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
                            activityController = activityController,
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