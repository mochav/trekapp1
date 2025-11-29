package com.example.trekapp1.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.trekapp1.controllers.ActivityController
import com.example.trekapp1.ui.theme.DarkBackground
import com.example.trekapp1.views.components.ActivityCardDirect

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

        // Check if there are activities
        if (activities.isEmpty()) {
            // Show "No data" message when list is empty
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No data",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        } else {
            // Display all activity cards using LazyColumn for better performance
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(activities) { activity ->
                    ActivityCardDirect(activity = activity)
                }
            }
        }
    }
}