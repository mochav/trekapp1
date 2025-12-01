package com.example.trekapp1.controllers

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.trekapp1.R
import com.example.trekapp1.TrekFirebase
import com.example.trekapp1.AvatarManagement
import com.example.trekapp1.localDatabase.LocalAvatar
import com.example.trekapp1.localDatabase.SyncManager
import com.example.trekapp1.models.Avatar
import com.example.trekapp1.models.UserAvatarProfile
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Controller for managing avatar shop logic and user avatar profile.
 * Handles avatar unlocking, equipping, and coin management.
 * Now integrated with Firebase and local database.
 *
 * IMPROVEMENTS:
 * - Checks if avatar is already unlocked before attempting purchase
 * - Updates UI immediately after purchase (optimistic updates)
 * - Better error messages for user clarity
 * - Prevents duplicate purchases
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
     * Success message for user feedback.
     */
    var successMessage by mutableStateOf<String?>(null)
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
     * Loads user avatar and coin data directly from Firebase.
     * Bypasses local database to ensure we get current state.
     */
    private fun loadUserData() {
        coroutineScope?.launch {
            isLoading = true
            error = null

            try {
                val uid = TrekFirebase.getCurrentUserId()
                Log.d("AvatarController", "Loading data for user: $uid")

                if (uid != null) {
                    withContext(Dispatchers.IO) {
                        val db = FirebaseFirestore.getInstance()

                        // Get coins directly from Firebase
                        val coinsDoc = db.collection("User Data")
                            .document(uid)
                            .collection("Coins")
                            .document("Balance")
                            .get()
                            .await()

                        val coins = (coinsDoc.getLong("Coins") ?: 0L).toInt()
                        Log.d("AvatarController", "Loaded coins: $coins")

                        // Get unlocked avatars directly from Firebase
                        val unlockedSnapshot = db.collection("User Data")
                            .document(uid)
                            .collection("Unlocked")
                            .get()
                            .await()

                        val unlockedIds = unlockedSnapshot.documents
                            .mapNotNull { it.getString("fileName") ?: it.id }
                            .toMutableSet()

                        Log.d("AvatarController", "Loaded unlocked avatars: $unlockedIds")

                        // Get selected avatar from user document
                        val userDoc = db.collection("User Data")
                            .document(uid)
                            .get()
                            .await()

                        val selectedAvatar = userDoc.getString("selectedAvatar")
                        Log.d("AvatarController", "Loaded selected avatar: $selectedAvatar")

                        // Update user profile on main thread
                        withContext(Dispatchers.Main) {
                            userProfile = userProfile.copy(
                                coins = coins,
                                unlockedAvatarIds = unlockedIds,
                                selectedAvatarId = selectedAvatar ?: unlockedIds.firstOrNull()
                            )
                            Log.d("AvatarController", "Updated userProfile - Coins: ${userProfile.coins}, Unlocked: ${userProfile.unlockedAvatarIds.size}, Selected: ${userProfile.selectedAvatarId}")
                        }
                    }
                } else {
                    Log.e("AvatarController", "No user ID found!")
                    error = "Not logged in"
                }
            } catch (e: Exception) {
                Log.e("AvatarController", "Error loading user data", e)
                error = "Failed to load avatar data: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * Handles avatar click events. Either equips an unlocked avatar,
     * unlocks and equips an affordable avatar, or shows appropriate message.
     *
     * IMPROVED: Now checks if already unlocked BEFORE attempting purchase
     *
     * @param avatar The avatar that was clicked.
     */
    fun handleAvatarClick(avatar: Avatar) {
        // Clear previous messages
        error = null
        successMessage = null

        Log.d("AvatarController", "=== Avatar Click ===")
        Log.d("AvatarController", "Clicked: ${avatar.name} (${avatar.id})")
        Log.d("AvatarController", "Price: ${avatar.price}")
        Log.d("AvatarController", "User coins: ${userProfile.coins}")
        Log.d("AvatarController", "Unlocked avatars: ${userProfile.unlockedAvatarIds}")

        val isUnlocked = avatar.id in userProfile.unlockedAvatarIds
        val isSelected = avatar.id == userProfile.selectedAvatarId
        val canAfford = userProfile.coins >= avatar.price

        Log.d("AvatarController", "Is unlocked: $isUnlocked")
        Log.d("AvatarController", "Is selected: $isSelected")
        Log.d("AvatarController", "Can afford: $canAfford")

        when {
            // If already selected, do nothing
            isSelected -> {
                Log.d("AvatarController", "ACTION: Already equipped")
                successMessage = "${avatar.name} is already equipped!"
            }
            // If already unlocked but not selected, equip it
            isUnlocked -> {
                Log.d("AvatarController", "ACTION: Equipping already unlocked avatar")
                equipAvatar(avatar)
            }
            // If locked but affordable, purchase it
            canAfford -> {
                Log.d("AvatarController", "ACTION: Purchasing locked avatar")
                purchaseAvatar(avatar)
            }
            // If locked and not affordable, show error
            else -> {
                Log.d("AvatarController", "ACTION: Cannot afford")
                val coinsNeeded = avatar.price - userProfile.coins
                error = "Not enough coins! You need $coinsNeeded more coins to unlock ${avatar.name}."
            }
        }
    }

    /**
     * Purchases an avatar using AvatarManagement and updates local state.
     * Uses optimistic UI updates for instant feedback.
     *
     * @param avatar The avatar to purchase.
     */
    private fun purchaseAvatar(avatar: Avatar) {
        coroutineScope?.launch(Dispatchers.Main) {
            isLoading = true
            error = null
            successMessage = null

            try {
                Log.d("AvatarController", "Starting purchase for: ${avatar.id}, cost: ${avatar.price}")

                // Call AvatarManagement
                AvatarManagement.boughtAvatar(
                    AvatarFile = avatar.id,
                    AvatarCost = avatar.price.toLong()
                ) { result ->
                    coroutineScope.launch(Dispatchers.Main) {
                        Log.d("AvatarController", "Purchase callback result: $result")

                        when (result) {
                            "Purchase Successful" -> {
                                Log.d("AvatarController", "Purchase successful! Updating UI immediately...")

                                // OPTIMISTIC UPDATE: Update UI immediately
                                val newUnlocked = userProfile.unlockedAvatarIds.toMutableSet()
                                newUnlocked.add(avatar.id)

                                userProfile = userProfile.copy(
                                    coins = userProfile.coins - avatar.price,
                                    unlockedAvatarIds = newUnlocked,
                                    selectedAvatarId = avatar.id  // Auto-equip newly purchased avatar
                                )

                                successMessage = "Unlocked ${avatar.name}!"
                                Log.d("AvatarController", "UI updated - new unlocked list: $newUnlocked")

                                // Reload from Firebase in background to confirm sync
                                loadUserData()
                            }
                            "Avatar Already Bought" -> {
                                Log.w("AvatarController", "Avatar already owned - this shouldn't happen!")
                                error = "You already own this avatar!"
                                // Reload to fix UI state
                                loadUserData()
                            }
                            else -> {
                                Log.e("AvatarController", "Purchase failed: $result")
                                error = "Purchase failed: $result"
                                isLoading = false
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AvatarController", "Error purchasing avatar", e)
                error = "Purchase failed: ${e.message}"
                isLoading = false
            }
        }
    }

    /**
     * Equips a selected avatar by updating the user document in Firebase.
     * Updates UI immediately for responsiveness.
     *
     * @param avatar The avatar to equip.
     */
    private fun equipAvatar(avatar: Avatar) {
        coroutineScope?.launch {
            try {
                Log.d("AvatarController", "Equipping avatar: ${avatar.id}")

                val uid = TrekFirebase.getCurrentUserId()
                if (uid != null) {
                    // OPTIMISTIC UPDATE: Update local state immediately
                    userProfile = userProfile.copy(selectedAvatarId = avatar.id)
                    successMessage = "Equipped ${avatar.name}!"
                    Log.d("AvatarController", "Local state updated, selectedAvatarId now: ${avatar.id}")

                    // Update Firebase in background
                    withContext(Dispatchers.IO) {
                        val db = FirebaseFirestore.getInstance()
                        db.collection("User Data")
                            .document(uid)
                            .update("selectedAvatar", avatar.id)
                            .await()

                        Log.d("AvatarController", "Firebase updated successfully with selectedAvatar: ${avatar.id}")
                    }
                } else {
                    Log.e("AvatarController", "Cannot equip - no user ID")
                    error = "Not logged in"
                }
            } catch (e: Exception) {
                Log.e("AvatarController", "Error equipping avatar", e)
                error = "Failed to equip avatar: ${e.message}"

                // Reload to get correct state
                loadUserData()
            }
        }
    }

    /**
     * Refreshes user data from the database.
     * Useful for pulling latest data after external changes.
     */
    fun refreshUserData() {
        Log.d("AvatarController", "Manual refresh requested")
        loadUserData()
    }

    /**
     * Clears error and success messages.
     * Call this from UI when user dismisses messages.
     */
    fun clearMessages() {
        error = null
        successMessage = null
    }
}