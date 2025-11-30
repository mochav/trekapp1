package com.example.trekapp1.controllers

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.trekapp1.R
import com.example.trekapp1.TrekFirebase
import com.example.trekapp1.AvatarManagement
import com.example.trekapp1.localDatabase.SyncManager
import com.example.trekapp1.models.Avatar
import com.example.trekapp1.models.UserAvatarProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Controller for managing avatar shop logic and user avatar profile.
 * Handles avatar unlocking, equipping, and coin management.
 * Now integrated with Firebase and local database.
 */
class AvatarController(
    private val coroutineScope: CoroutineScope? = null
) {

    /**
     * Current user profile containing coins and unlocked avatars.
     * Exposed as Compose state for automatic recomposition.
     */
    var userProfile by mutableStateOf(
        UserAvatarProfile(
            coins = 0,
            unlockedAvatarIds = mutableSetOf(),
            selectedAvatarId = null
        )
    )
        private set

    /**
     * Loading state for UI feedback.
     */
    var isLoading by mutableStateOf(false)
        private set

    /**
     * Error message if avatar operations fail.
     */
    var error by mutableStateOf<String?>(null)
        private set

    /**
     * List of all available avatars in the shop.
     */
    val avatars = listOf(
        Avatar("bigrun.PNG", "Trail Runner", 0, R.drawable.bigrun),
        Avatar("imwalkin.PNG", "Night Sprinter", 300, R.drawable.imwalkin),
        Avatar("plane.PNG", "Mountain Climber", 500, R.drawable.plane),
        Avatar("kingrun.PNG", "Speed Demon", 750, R.drawable.kingrun),
        Avatar("partner.PNG", "Zen Jogger", 1000, R.drawable.partner),
        Avatar("blackwhiterunner.PNG", "Cyber Runner", 1250, R.drawable.blackwhiterunner),
        Avatar("apple.PNG", "Forest Guardian", 1500, R.drawable.apple),
        Avatar("shoe.PNG", "Urban Explorer", 2000, R.drawable.shoe),
        Avatar("mustacherunner.PNG", "Desert Nomad", 2500, R.drawable.mustacherunner),
    )

    init {
        // Load user data from database
        loadUserData()
    }

    /**
     * Loads user avatar and coin data from the database.
     * Should be called from a coroutine scope.
     */
    private fun loadUserData() {
        if (coroutineScope != null) {
            coroutineScope.launch {
                isLoading = true
                error = null

                try {
                    val uid = TrekFirebase.getCurrentUserId()
                    if (uid != null) {
                        // Get coins from local database
                        val coinsFlow = SyncManager.getCoinsFlow(uid)
                        val coinBalance = coinsFlow.firstOrNull()
                        val coins = coinBalance?.coins?.toInt() ?: 0

                        // Get unlocked avatars from local database
                        val unlockedFlow = SyncManager.getUnlockedAvatarsFlow()
                        val unlockedAvatars = unlockedFlow.firstOrNull() ?: emptyList()
                        val unlockedIds = unlockedAvatars.map { it.fileName }.toMutableSet()

                        // Get selected avatar from user document
                        val userFlow = SyncManager.userDao().getUserFlow(uid)
                        val user = userFlow.firstOrNull()
                        val selectedAvatar = user?.selectedAvatar

                        // Update user profile
                        userProfile = userProfile.copy(
                            coins = coins,
                            unlockedAvatarIds = unlockedIds,
                            selectedAvatarId = selectedAvatar ?: unlockedIds.firstOrNull()
                        )
                    }
                } catch (e: Exception) {
                    error = e.message
                } finally {
                    isLoading = false
                }
            }
        }
    }

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
                equipAvatar(avatar.id)
            }
            // If locked but affordable, unlock and equip it
            canAfford -> {
                purchaseAvatar(avatar)
            }
            // If locked and not affordable, show error
            else -> {
                error = "Not enough coins to purchase ${avatar.name}"
            }
        }
    }

    /**
     * Purchases an avatar using AvatarManagement and updates local state.
     *
     * @param avatar The avatar to purchase.
     */
    private fun purchaseAvatar(avatar: Avatar) {
        if (coroutineScope != null) {
            coroutineScope.launch {
                isLoading = true
                error = null

                try {
                    withContext(Dispatchers.IO) {
                        AvatarManagement.boughtAvatar(
                            AvatarFile = avatar.id,
                            AvatarCost = avatar.price.toLong()
                        ) { result ->
                            coroutineScope.launch {
                                if (result == "Purchase Successful") {
                                    // Reload user data to reflect changes
                                    loadUserData()

                                    // Update local state optimistically
                                    val newUnlocked = userProfile.unlockedAvatarIds.toMutableSet()
                                    newUnlocked.add(avatar.id)
                                    userProfile = userProfile.copy(
                                        coins = userProfile.coins - avatar.price,
                                        unlockedAvatarIds = newUnlocked,
                                        selectedAvatarId = avatar.id
                                    )
                                } else {
                                    error = result
                                }
                                isLoading = false
                            }
                        }
                    }
                } catch (e: Exception) {
                    error = e.message
                    isLoading = false
                }
            }
        }
    }

    /**
     * Equips a selected avatar by updating the user document in Firebase.
     *
     * @param avatarId The ID of the avatar to equip.
     */
    private fun equipAvatar(avatarId: String) {
        if (coroutineScope != null) {
            coroutineScope.launch {
                try {
                    val uid = TrekFirebase.getCurrentUserId()
                    if (uid != null) {
                        // Update Firebase user document
                        withContext(Dispatchers.IO) {
                            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            db.collection("User Data")
                                .document(uid)
                                .update("selectedAvatar", avatarId)
                                .addOnSuccessListener {
                                    // Update local state
                                    userProfile = userProfile.copy(selectedAvatarId = avatarId)
                                }
                                .addOnFailureListener { e ->
                                    error = e.message
                                }
                        }
                    }
                } catch (e: Exception) {
                    error = e.message
                }
            }
        }
    }

    /**
     * Refreshes user data from the database.
     * Should be called when returning to the avatar view.
     */
    suspend fun refreshUserData() {
        loadUserData()
    }

    /**
     * Adds coins to the user's profile (for testing or rewards).
     * Updates both Firebase and local database.
     *
     * @param amount Number of coins to add.
     */
    fun addCoins(amount: Int) {
        if (coroutineScope != null) {
            coroutineScope.launch {
                try {
                    val uid = TrekFirebase.getCurrentUserId()
                    if (uid != null) {
                        withContext(Dispatchers.IO) {
                            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            db.collection("User Data")
                                .document(uid)
                                .collection("Coins")
                                .document("Balance")
                                .update("Coins", com.google.firebase.firestore.FieldValue.increment(amount.toLong()))
                                .addOnSuccessListener {
                                    // Local DB will be updated via SyncManager listener
                                    loadUserData()
                                }
                                .addOnFailureListener { e ->
                                    error = e.message
                                }
                        }
                    }
                } catch (e: Exception) {
                    error = e.message
                }
            }
        }
    }
}