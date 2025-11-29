package com.example.trekapp1

import android.util.Log
import com.example.trekapp1.TrekFirebase.getCurrentUserId
import com.example.trekapp1.localDatabase.LocalAvatar
import com.example.trekapp1.localDatabase.LocalCoinBalance
import com.example.trekapp1.localDatabase.SyncManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


/* File: AvatarManagement.kt
 * Author: Clayton Frandeen
 * Last Date Updated: November 25th, 2025
 * Purpose: To manage the transaction between app and database
 *          when user purchases an avatar. Check appropriate amount of coins,
 *          and to move from locked to unlocked
 * Parameters:
 *      @AvatarFile: String - the name of the file the user is trying to purchase
 *      @AvatarCost: Long - the cost to purchase the image file
 *      @onResult: To show messages like purchase successful and error messages
 * Use Example:
 *  boughtAvatar("apple.PNG", 500) { result ->
 *   Toast.makeText(context, result, Toast.LENGTH_SHORT).show()
 *  }
 */
object AvatarManagement {
    private fun auth(): FirebaseAuth = FirebaseAuth.getInstance()
    private fun db(): FirebaseFirestore = FirebaseFirestore.getInstance()

    fun boughtAvatar (AvatarFile: String, AvatarCost: Long, onResult: (String) -> Unit) {
        val db = db()
        val uid = getCurrentUserId() ?: return onResult("User not logged in")

        val userRef = db.collection("User Data").document(uid)
        val coinsRef = userRef.collection("Coins").document("Balance")
        val lockedRef = userRef.collection( "Locked").document(AvatarFile)
        val unlockedRef = userRef.collection("Unlocked").document(AvatarFile)

        db.runTransaction { transaction ->
            // Read Coins
            val coinSnap = transaction.get(coinsRef)
            val currentCoins = coinSnap.getLong("Coins") ?: 0L

            // If not enough coins
            if (currentCoins < AvatarCost) {
                throw Exception("NOT_ENOUGH_COINS")
            }

            // Get locked avatar
            val lockedSnap = transaction.get(lockedRef)
            if (!lockedSnap.exists()) {
                throw Exception("AVATAR_PREVIOUSLY_BOUGHT")
            }

            // Decrement User Coins
            transaction.update(coinsRef, "Coins", currentCoins - AvatarCost)

            // Write to unlocked
            transaction.set(unlockedRef, AvatarFile)

            // Delete from locked
            transaction.delete(lockedRef)

            "SUCCESS"
        }
            .addOnSuccessListener {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // update coins locally
                        val coinsSnap =
                            userRef.collection("Coins").document("Balance").get().await()
                        val newCoins = (coinsSnap.getLong("Coins") ?: 0L)
                        SyncManager.coinsDao().insert(LocalCoinBalance(
                            uid = uid,
                            coins = newCoins
                        ))
                        SyncManager.avatarDao().insert(LocalAvatar(
                            fileName = AvatarFile,
                            locked = false))
                    } catch (e: Exception) {
                        Log.e("AvatarManagement", "Error updating local DB after purchase: ${e.message}")
                    }
                }
                onResult("Purchase Successful")

            }
            .addOnFailureListener { e ->
                when(e.message) {
                    "NOT_ENOUGH_COINS" -> onResult("Not Enough Coins")
                    "AVATAR_PREVIOUSLY_BOUGHT" -> onResult("Avatar Already Bought")
                    else -> onResult("Error: ${e.message}")
                }
            }
    }
}