package com.example.trekapp1.controllers

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.trekapp1.R
import com.example.trekapp1.models.Avatar
import com.example.trekapp1.models.UserAvatarProfile

/**
 * Controller for managing avatar shop logic and user avatar profile.
 * Handles avatar unlocking, equipping, and coin management.
 */
class AvatarController {

    /**
     * Current user profile containing coins and unlocked avatars.
     * Exposed as Compose state for automatic recomposition.
     */
    var userProfile by mutableStateOf(
        UserAvatarProfile(
            coins = 1500,
            unlockedAvatarIds = mutableSetOf("trail_runner"),
            selectedAvatarId = "trail_runner"
        )
    )
        private set

    /**
     * List of all available avatars in the shop.
     */
    val avatars = listOf(
        Avatar("trail_runner", "Trail Runner", 0, R.drawable.bigrun),
        Avatar("night_sprinter", "Night Sprinter", 300, R.drawable.imwalkin),
        Avatar("mountain_climber", "Mountain Climber", 500, R.drawable.plane),
        Avatar("speed_demon", "Speed Demon", 750, R.drawable.kingrun),
        Avatar("zen_jogger", "Zen Jogger", 1000, R.drawable.partner),
        Avatar("cyber_runner", "Cyber Runner", 1250, R.drawable.blackwhiterunner),
        Avatar("forest_guardian", "Forest Guardian", 1500, R.drawable.apple),
        Avatar("urban_explorer", "Urban Explorer", 2000, R.drawable.shoe),
        Avatar("desert_nomad", "Desert Nomad", 2500, R.drawable.mustacherunner),
    )

    /**
     * Handles avatar click events. Either equips an unlocked avatar,
     * unlocks and equips an affordable avatar, or does nothing if unaffordable.
     *
     * @param avatar The avatar that was clicked.
     */
    fun handleAvatarClick(avatar: Avatar) {
        val isUnlocked = avatar.id in userProfile.unlockedAvatarIds
        val canAfford = userProfile.coins >= avatar.price

        when {
            // If already unlocked, equip it
            isUnlocked -> {
                userProfile = userProfile.copy(selectedAvatarId = avatar.id)
            }
            // If locked but affordable, unlock and equip it
            canAfford -> {
                val newUnlocked = userProfile.unlockedAvatarIds.toMutableSet()
                newUnlocked.add(avatar.id)
                userProfile = userProfile.copy(
                    coins = userProfile.coins - avatar.price,
                    unlockedAvatarIds = newUnlocked,
                    selectedAvatarId = avatar.id
                )
            }
            // If locked and not affordable, do nothing
            else -> {
                // TODO: Show error message to user
            }
        }
    }

    /**
     * Adds coins to the user's profile.
     * Called when user completes activities.
     *
     * @param amount Number of coins to add.
     */
    fun addCoins(amount: Int) {
        userProfile = userProfile.copy(coins = userProfile.coins + amount)
    }
}