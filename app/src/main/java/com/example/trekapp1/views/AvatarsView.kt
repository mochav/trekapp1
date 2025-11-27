package com.example.trekapp1.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.trekapp1.controllers.AvatarController
import com.example.trekapp1.ui.theme.CardBackground
import com.example.trekapp1.ui.theme.CoralOrange
import com.example.trekapp1.ui.theme.CoralPink
import com.example.trekapp1.ui.theme.DarkBackground
import com.example.trekapp1.views.components.AvatarCard

/**
 * Avatars view displaying the avatar shop.
 * Shows user's coins and available avatars for purchase/equipping.
 *
 * @param avatarController Controller for avatar shop logic.
 */
@Composable
fun AvatarsView(
    avatarController: AvatarController
) {
    val userProfile = avatarController.userProfile
    val avatars = avatarController.avatars

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        // Header
        Text(
            "Avatar Shop",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Earn coins by walking and running to unlock new avatars",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Coins Display Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Your Coins",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${userProfile.coins}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = CoralPink
                    )
                }
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(CoralOrange, CoralPink)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Avatar Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(avatars.size) { index ->
                AvatarCard(
                    avatar = avatars[index],
                    isUnlocked = avatars[index].id in userProfile.unlockedAvatarIds,
                    isSelected = avatars[index].id == userProfile.selectedAvatarId,
                    canAfford = userProfile.coins >= avatars[index].price,
                    onAvatarClick = {
                        avatarController.handleAvatarClick(avatars[index])
                    }
                )
            }
        }
    }
}