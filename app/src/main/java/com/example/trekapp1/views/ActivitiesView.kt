package com.example.trekapp1.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.trekapp1.controllers.ActivityController
import com.example.trekapp1.ui.theme.DarkBackground
import com.example.trekapp1.views.components.ActivityCard

/**
 * Activities view showing complete list of all user activities.
 * Displays all recorded running and walking sessions.
 *
 * @param activityController Controller for accessing activity records.
 */
@Composable
fun ActivitiesView(
    activityController: ActivityController
) {
    val activities = activityController.getAllActivities()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        // Header
        Text(
            "All Activities",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Display all activity cards
        activities.forEach { activity ->
            ActivityCard(activity = activity)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}