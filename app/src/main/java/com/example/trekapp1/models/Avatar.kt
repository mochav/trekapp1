package com.example.trekapp1.models

/**
 * Represents an avatar that can be unlocked and equipped by users.
 *
 * @property id Unique identifier for the avatar.
 * @property name Display name of the avatar.
 * @property price Cost in coins to unlock this avatar.
 * @property imageResId Resource ID of the avatar's image drawable.
 */
data class Avatar(
    val id: String,
    val name: String,
    val price: Int,
    val imageResId: Int
)