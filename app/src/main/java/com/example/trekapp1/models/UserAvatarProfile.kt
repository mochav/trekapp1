package com.example.trekapp1.models

/**
 * Represents a user's avatar profile including coins and unlocked avatars.
 *
 * @property coins Current number of coins the user has earned.
 * @property unlockedAvatarIds Set of avatar IDs that the user has unlocked.
 * @property selectedAvatarId ID of the currently equipped avatar, null if none selected.
 */
data class UserAvatarProfile(
    var coins: Int,
    val unlockedAvatarIds: MutableSet<String>,
    var selectedAvatarId: String?
)