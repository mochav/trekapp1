package com.example.trekapp1.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trekapp1.controllers.ActivityController
import com.example.trekapp1.controllers.DashboardController
import com.example.trekapp1.ui.theme.CoralOrange
import com.example.trekapp1.ui.theme.CoralPink
import com.example.trekapp1.ui.theme.DarkBackground
import com.example.trekapp1.views.components.ActivityCard
import com.example.trekapp1.views.components.StatCard

/**
 * Dashboard view showing overview statistics and recent activities.
 * Displays user's current stats and provides option to start a new run.
 *
 * @param dashboardController Controller for dashboard statistics.
 * @param activityController Controller for activity records.
 * @param onStartRun Callback when user clicks "Start Run" button.
 */
@Composable
fun DashboardView(
    dashboardController: DashboardController,
    activityController: ActivityController,
    onStartRun: () -> Unit
) {
    val stats = dashboardController.stats
    val recentActivities = activityController.getRecentActivities()

    // Load stats when view appears
    LaunchedEffect(Unit) {
        dashboardController.loadStats()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        // Stats Cards Row 1
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Distance",
                value = stats.distance,
                unit = "km",
                icon = Icons.Default.DirectionsRun
            )
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Steps",
                value = stats.steps,
                unit = "steps",
                icon = Icons.Default.Man
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Stats Cards Row 2
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Calories",
                value = stats.calories,
                unit = "kcal",
                icon = Icons.Default.LocalFireDepartment
            )
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Pace",
                value = stats.pace,
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

        // Display recent activity cards
        recentActivities.forEach { activity ->
            ActivityCard(activity = activity)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}