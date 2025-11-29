package com.example.trekapp1.views.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.trekapp1.controllers.AvatarController
import com.example.trekapp1.ui.theme.CoralOrange
import com.example.trekapp1.ui.theme.CoralPink

/**
 * Displays the currently selected avatar in a circular frame.
 * Shows in the top-right corner of the app bar.
 * Only displays unlocked avatars.
 *
 * @param avatarController The controller managing avatar state.
 */
@Composable
fun SelectedAvatarDisplay(avatarController: AvatarController) {
    val userProfile = avatarController.userProfile
    val selectedAvatar = avatarController.avatars.find { it.id == userProfile.selectedAvatarId }

    // Only show if there's a selected avatar and it's unlocked
    if (selectedAvatar != null && selectedAvatar.id in userProfile.unlockedAvatarIds) {
        Box(
            modifier = Modifier
                .padding(end = 12.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(CoralOrange, CoralPink)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = selectedAvatar.imageResId),
                contentDescription = "Selected Avatar",
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }
    }
}