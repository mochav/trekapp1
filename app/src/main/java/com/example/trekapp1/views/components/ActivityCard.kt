package com.example.trekapp1.views.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.trekapp1.models.ActivityRecord
import com.example.trekapp1.ui.theme.CardBackground
import com.example.trekapp1.ui.theme.CoralPink

/**
 * Card displaying information about a single activity record.
 * Loads data from database and shows "No data" if empty.
 *
 * @param activityId The ID of the activity to load from database.
 * @param loadActivity Suspend function to load activity from database.
 * @param modifier Modifier for the card.
 */
@Composable
fun ActivityCard(
    activityId: Long,
    loadActivity: suspend (Long) -> ActivityRecord?,
    modifier: Modifier = Modifier
) {
    var activity by remember { mutableStateOf<ActivityRecord?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(activityId) {
        isLoading = true
        activity = loadActivity(activityId)
        isLoading = false
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = CardBackground
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        color = CoralPink,
                        modifier = Modifier.size(24.dp)
                    )
                }
                activity == null -> {
                    Text(
                        "No data",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
                else -> {
                    ActivityCardContent(activity = activity!!)
                }
            }
        }
    }
}

/**
 * Card that accepts ActivityRecord directly.
 * Useful when data is already loaded from database.
 *
 * @param activity The activity record to display, or null if no data.
 * @param modifier Modifier for the card.
 */
@Composable
fun ActivityCardDirect(
    activity: ActivityRecord?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = CardBackground
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (activity == null) {
                Text(
                    "No data",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.5f)
                )
            } else {
                ActivityCardContent(activity = activity)
            }
        }
    }
}

/**
 * Shared content component for displaying activity details.
 * Used by both ActivityCard and ActivityCardDirect.
 */
@Composable
private fun ActivityCardContent(activity: ActivityRecord) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                activity.date,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                activity.distance,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CoralPink
            )
        }
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                activity.duration,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                activity.pace,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}