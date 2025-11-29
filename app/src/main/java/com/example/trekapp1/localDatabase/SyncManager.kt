package com.example.trekapp1.localDatabase

import android.content.Context
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * SyncManager
 * - initialize(context)
 * - startUserSync(uid)
 * - stopUserSync()
 *
 * The manager listens to Firestore and writes changes to Room.
 * The UI should observe Room via DAOs / Flows.
 */
object SyncManager {
    private var dbLocal: LocalDatabase? = null
    private var firestore = FirebaseFirestore.getInstance()
    private var userSnapshotListeners: MutableList<() -> Unit> = mutableListOf()
    private var currentUid: String? = null

    fun initialize(appContext: Context) {
        // Use applicationContext, but DO NOT store the context
        dbLocal = LocalDatabase.getInstance(appContext.applicationContext)
    }

    private fun ensureInitialized(): LocalDatabase {
        return dbLocal ?: throw IllegalStateException(
            "SyncManager not initialized. Call SyncManager.initialize(context) first."
        )
    }

    fun startUserSync(uid: String) {
        if (currentUid == uid) return
        stopUserSync()
        currentUid = uid

        // Sync totals document
        val totalsRef = firestore.collection("User Data").document(uid).collection("Health Data").document("Total")
        val totalsListener = totalsRef.addSnapshotListener { doc, err ->
            if (err != null) {
                Log.e("SyncManager", "Totals listen error", err)
                return@addSnapshotListener
            }
            if (doc != null && doc.exists()) {
                val steps = (doc.get("steps") as? Number)?.toLong() ?: 0L
                val miles = (doc.get("miles") as? Number)?.toDouble() ?: 0.0
                val calories = (doc.get("calories") as? Number)?.toLong() ?: 0L
                val updatedAt = (doc.get("updatedAt") as? Timestamp)?.seconds ?: System.currentTimeMillis() / 1000
                val local = LocalUserTotals(uid = uid, steps = steps, miles = miles, calories = calories, updatedAt = updatedAt)
                CoroutineScope(Dispatchers.IO).launch {
                    ensureInitialized().userTotalsDao().insert(local)
                }
            }
        }
        // store removal handle
        userSnapshotListeners.add { totalsListener.remove() }

        // Sync daily document for today
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val dailyRef = firestore.collection("User Data").document(uid).collection("Health Data").document(today)
        val dailyListener = dailyRef.addSnapshotListener { doc, err ->
            if (err != null) {
                Log.e("SyncManager", "Daily listen error", err)
                return@addSnapshotListener
            }
            if (doc != null && doc.exists()) {
                val steps = (doc.get("steps") as? Number)?.toLong() ?: 0L
                val miles = (doc.get("miles") as? Number)?.toDouble() ?: 0.0
                val calories = (doc.get("calories") as? Number)?.toLong() ?: 0L
                val id = "${uid}_$today"
                val local = LocalDailyData(id = id, uid = uid, date = today, steps = steps, miles = miles, calories = calories)
                CoroutineScope(Dispatchers.IO).launch {
                    ensureInitialized().dailyDataDao().insert(local)
                }
            }
        }
        userSnapshotListeners.add { dailyListener.remove() }

        // Sync coins
        val coinsRef = firestore.collection("User Data").document(uid).collection("Coins").document("Balance")
        val coinsListener = coinsRef.addSnapshotListener { doc, err ->
            if (err != null) {
                Log.e("SyncManager", "Coins listen error", err)
                return@addSnapshotListener
            }
            if (doc != null && doc.exists()) {
                val coins = (doc.get("Coins") as? Number)?.toLong() ?: 0L
                val local = LocalCoinBalance(uid = uid, coins = coins)
                CoroutineScope(Dispatchers.IO).launch {
                    ensureInitialized().coinsDao().insert(local)
                }
            }
        }
        userSnapshotListeners.add { coinsListener.remove() }

        // Sync avatars (locked/unlocked)
        val lockedRef = firestore.collection("User Data").document(uid).collection("Locked")
        val lockedListener = lockedRef.addSnapshotListener { snap, err ->
            if (err != null) {
                Log.e("SyncManager", "Locked listen error", err)
                return@addSnapshotListener
            }
            if (snap != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    val dao = ensureInitialized().avatarDao()
                    // clear and repopulate (simple approach)
                    dao.clearAll()
                    // Add locked
                    for (doc in snap.documents) {
                        val fileName = doc.getString("fileName") ?: doc.id
                        dao.insert(LocalAvatar(fileName = fileName, locked = true))
                    }
                }
            }
        }
        userSnapshotListeners.add { lockedListener.remove() }

        val unlockedRef = firestore.collection("User Data").document(uid).collection("Unlocked")
        val unlockedListener = unlockedRef.addSnapshotListener { snap, err ->
            if (err != null) {
                Log.e("SyncManager", "Unlocked listen error", err)
                return@addSnapshotListener
            }
            if (snap != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    val dao = ensureInitialized().avatarDao()
                    for (doc in snap.documents) {
                        val fileName = doc.getString("fileName") ?: doc.id
                        dao.insert(LocalAvatar(fileName = fileName, locked = false))
                    }
                }
            }
        }
        userSnapshotListeners.add { unlockedListener.remove() }

        // Optionally sync basic user doc (email, selected avatar)
        val userDocRef = firestore.collection("User Data").document(uid)
        val userDocListener = userDocRef.addSnapshotListener { doc, err ->
            if (err != null) {
                Log.e("SyncManager", "User doc listen error", err)
                return@addSnapshotListener
            }
            if (doc != null && doc.exists()) {
                val email = doc.getString("email")
                val selectedAvatar = doc.getString("selectedAvatar")
                CoroutineScope(Dispatchers.IO).launch {
                    ensureInitialized().userDao().insert(LocalUser(uid = uid, email = email, selectedAvatar = selectedAvatar))
                }
            }
        }
        userSnapshotListeners.add { userDocListener.remove() }

        Log.d("SyncManager", "Started sync for user $uid")
    }

    fun stopUserSync() {
        // remove all listeners
        for (remover in userSnapshotListeners) {
            try {
                remover()
            } catch (e: Exception) {
                // ignore
            }
        }
        userSnapshotListeners.clear()
        currentUid = null
        Log.d("SyncManager", "Stopped user sync")
    }

    // Convenience getters for DAOs
    fun userDao() = ensureInitialized().userDao()
    fun userTotalsDao() = ensureInitialized().userTotalsDao()
    fun dailyDataDao() = ensureInitialized().dailyDataDao()
    fun avatarDao() = ensureInitialized().avatarDao()
    fun coinsDao() = ensureInitialized().coinsDao()

    // One-shot helper: fetch user files (locked/unlocked) and write to local DB
    suspend fun fetchUserFilesOnce(uid: String) {
        val lockedRef = firestore.collection("User Data").document(uid).collection("Locked")
        val unlockedRef = firestore.collection("User Data").document(uid).collection("Unlocked")
        val lockedSnap = lockedRef.get().await()
        val unlockedSnap = unlockedRef.get().await()
        val dao = ensureInitialized().avatarDao()
        // clear and insert
        dao.clearAll()
        for (d in lockedSnap.documents) {
            val fname = d.getString("fileName") ?: d.id
            dao.insert(LocalAvatar(fileName = fname, locked = true))
        }
        for (d in unlockedSnap.documents) {
            val fname = d.getString("fileName") ?: d.id
            dao.insert(LocalAvatar(fileName = fname, locked = false))
        }
    }

    // Utility: run a local read quickly for UI
    fun getTotalsFlow(uid: String) = userTotalsDao().getTotalsFlow(uid)
    fun getDailyFlow(uid: String, date: String) = dailyDataDao().getDailyFlow(uid, date)
    fun getCoinsFlow(uid: String) = coinsDao().getCoinsFlow(uid)
    fun getLockedAvatarsFlow() = avatarDao().getLockedFlow()
    fun getUnlockedAvatarsFlow() = avatarDao().getUnlockedFlow()
}
