package com.example.trekapp1.views.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trekapp1.models.Avatar
import com.example.trekapp1.ui.theme.CardBackground
import com.example.trekapp1.ui.theme.CoralOrange
import com.example.trekapp1.ui.theme.CoralPink

/**
 * Card displaying an avatar in the shop.
 * Shows avatar image, name, price, and unlock status.
 *
 * @param avatar The avatar data to display.
 * @param isUnlocked Whether the user has unlocked this avatar.
 * @param isSelected Whether this avatar is currently equipped.
 * @param canAfford Whether the user has enough coins to unlock this avatar.
 * @param onAvatarClick Callback when the avatar card is clicked.
 * @param modifier Modifier for the card.
 */
@Composable
fun AvatarCard(
    avatar: Avatar,
    isUnlocked: Boolean,
    isSelected: Boolean,
    canAfford: Boolean,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onAvatarClick,
        enabled = isUnlocked || canAfford,
        modifier = modifier.aspectRatio(1f),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                CoralPink.copy(alpha = 0.3f)
            } else {
                CardBackground
            }
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected) {
            BorderStroke(2.dp, CoralPink)
        } else null
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(8.dp)
            ) {
                // Avatar Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (isUnlocked) {
                                Brush.linearGradient(
                                    colors = listOf(CoralOrange, CoralPink)
                                )
                            } else {
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color.Gray.copy(alpha = 0.3f),
                                        Color.Gray.copy(alpha = 0.5f)
                                    )
                                )
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = avatar.imageResId),
                        contentDescription = avatar.name,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Avatar Name
                Text(
                    avatar.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isUnlocked) Color.White else Color.Gray,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Price or Status
                if (!isUnlocked) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = if (canAfford) CoralPink else Color.Gray
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            "${avatar.price}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (canAfford) CoralPink else Color.Gray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                } else if (isSelected) {
                    Text(
                        "EQUIPPED",
                        style = MaterialTheme.typography.bodySmall,
                        color = CoralPink,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                } else {
                    Text(
                        "Unlocked",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 10.sp
                    )
                }
            }

            // Lock overlay for locked & unaffordable avatars
            if (!isUnlocked && !canAfford) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Locked",
                        modifier = Modifier.size(32.dp),
                        tint = Color.Gray
                    )
                }
            }
        }
    }
}