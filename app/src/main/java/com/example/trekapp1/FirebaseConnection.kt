package com.example.trekapp1

import android.util.Log
import com.example.trekapp1.localDatabase.LocalCoinBalance
import com.example.trekapp1.localDatabase.LocalDailyData
import com.example.trekapp1.localDatabase.LocalUser
import com.example.trekapp1.localDatabase.LocalUserTotals
import com.example.trekapp1.localDatabase.SyncManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.floor


/* File: FirebaseConnection.kt
 * Author: Clayton Frandeen
 * Last Date Updated: November 25th, 2025
 * Purpose: To manage the transaction between app and database
 *          when user creates an account and reads or writes data
 */
object TrekFirebase {

    val avatarFiles = listOf("apple.PNG", "bigrun.PNG", "blackwhiterunner.PNG", "imwalkin.PNG", "kingrun.PNG", "mustacherunner.PNG", "partner.PNG", "plane.PNG", "shoe.PNG")
    private fun auth(): FirebaseAuth = FirebaseAuth.getInstance()
    private fun db(): FirebaseFirestore = FirebaseFirestore.getInstance()

    // Below deals with user accounts

    /* Function: Register user
     * Parameters:
     *      @email: String - users email
     *      @password: String - users password
     *      @onResult - throws error and success messages
     * Purpose: Creates a user authentication through firestore and creates
     *          initial documents in database
     * Use Example: (Add to sign up button)
     *      registerUser(email, password) {result ->
     *         Toast.makeText(context, result, Toast.LENGTH_SHORT).show()
     *      }
     */
    fun registerUser(
        email: String,
        password: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val auth = auth()
        val db = db()

        // Creates User with email and password
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid ?: ""
                    // creating a document for this user
                    val userDoc = db.collection("User Data").document(uid)
                    val initialData = hashMapOf(
                        "UserUID" to uid,
                        "email" to email
                    )
                    // Starts a total document, to collect a cumulation of all health data
                    userDoc.set(initialData)
                        .addOnSuccessListener {
                            val healthDataRef = userDoc.collection("Health Data")
                            // Inititalize to zero
                            val totalDoc = hashMapOf(
                                "steps" to 0,
                                "miles" to 0.0,
                                "calories" to 0,
                                "updatedAt" to com.google.firebase.Timestamp.now()
                            )
                            healthDataRef.document("Total").set(totalDoc)
                                .addOnSuccessListener {
                                    initializeUserFiles(uid, avatarFiles)
                                    createUserCoins(uid)
                                    // write user to Room
                                    CoroutineScope(Dispatchers.IO).launch {
                                        try {
                                            SyncManager.userDao().insert(
                                                LocalUser(
                                                    uid = uid,
                                                    email = email
                                                )
                                            )
                                            SyncManager.userTotalsDao().insert(
                                                LocalUserTotals(
                                                    uid = uid,
                                                    steps = 0,
                                                    miles = 0.0,
                                                    calories = 0
                                                )
                                            )
                                            SyncManager.coinsDao().insert(
                                                LocalCoinBalance(
                                                    uid = uid,
                                                    coins = 0
                                                )
                                            )
                                        } catch (e: Exception) {
                                            Log.e("TrekFirebase", "Error seeding local DB: ${e.message}")
                                        }
                                    }
                                    onResult(true, null)
                                }
                                .addOnFailureListener { e ->
                                    onResult(false, e.message)
                                }
                        }
                        .addOnFailureListener { e->
                            onResult(false, e.message)
                        }

                } else {
                    onResult(false, task.exception?.message)
                }
            }
    }

    /* Helper Function: createUserCoins
     * Parameters:
     *      @userId: String - users id for database, accessible by calling
     *                        getCurrentUserId()
     */
    private fun createUserCoins(userId: String) {
        val db = FirebaseFirestore.getInstance()

        val coinsDoc = db.collection("User Data")
            .document(userId)
            .collection("Coins")
            .document("Balance")

        val data = mapOf("Coins" to 0)

        coinsDoc.set(data)
            .addOnSuccessListener { println("Coins initialized") }
            .addOnFailureListener { println("Error: $it") }
    }

    /* Helper Function: initializeUserFiles
     * Parameters:
     *      @userId: String - users id for database, accessible by calling
     *                        getCurrentUserId()
     */
    private fun initializeUserFiles(userId: String, fileNames: List<String>) {
        val db = FirebaseFirestore.getInstance()
        val lockedCollection = db.collection("User Data")
            .document(userId)
            .collection("Locked")

        val batch = db.batch()

        fileNames.forEach { name ->
            val docRef = lockedCollection.document(name) // auto ID
            batch.set(docRef, mapOf("fileName" to name))
        }

        batch.commit()
            .addOnSuccessListener { println("Files added to locked") }
            .addOnFailureListener { println("Error: $it") }
    }

    /* Function: LoginUser
     * Parameters:
     *      @email: String - users email
     *      @password: String - users password
     *      @onResult - throws error and success messages
     * Purpose: Validates user info and signs into database
     * Use Example: (Add to login button)
     *      loginUser(email, password) {result ->
     *         Toast.makeText(context, result, Toast.LENGTH_SHORT).show()
     *      }
     */
    fun loginUser(
        email: String,
        password: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val auth = auth()
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    onResult(false, task.exception?.message)
                }
            }
    }

    /* Function: signOut
     * Purpose: Can add to a logout button, just call function
     */
    fun signOut() {
        val auth = auth()
        auth.signOut()
    }

    fun isUserLoggedIn(): Boolean {
        val auth = auth()
        return auth.currentUser != null
    }

    fun getCurrentUserId(): String? {
        val auth = auth()
        return auth.currentUser?.uid
    }

    // Below deals with users data

    // Updates database with health data, takes most recent steps and adds it to the running totals
    /* Function: logActivity
     * Parameters:
     *      @steps: Long
     *      @miles: Double
     *      @calories: Long
     * Purpose: updates database when collecting users health data
     *          adds these values ONTO the values already in db
     *          also updates users coins by the amount of steps taken
     *
     * COIN RETURN RATIO: Current - 1:1 coins to steps
     */
    fun logActivity(steps: Long, miles: Double, calories: Long) {
        val db = com.example.trekapp1.TrekFirebase.db()
        val uid = com.example.trekapp1.TrekFirebase.getCurrentUserId() ?: return
        val userDocRef = db.collection("User Data").document(uid)
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())

        val dailyRef = userDocRef.collection("Health Data").document(today)
        val totalRef = userDocRef.collection("Health Data").document("Total")
        val coinsRef = userDocRef.collection("Coins").document("balance")

        db.runTransaction { transaction ->
            // --- Increment DAILY counters ---
            transaction.set(
                dailyRef,
                mapOf(
                    "steps" to FieldValue.increment(steps),
                    "miles" to FieldValue.increment(miles),
                    "calories" to FieldValue.increment(calories),
                    "date" to today
                ),
                SetOptions.merge() // Merge with existing document if it exists
            )

            // --- Increment TOTAL counters ---
            transaction.set(
                totalRef,
                mapOf(
                    "steps" to FieldValue.increment(steps),
                    "miles" to FieldValue.increment(miles),
                    "calories" to FieldValue.increment(calories)
                ),
                SetOptions.merge()
            )

            // --- Increment COINS ---
            val newCoins = floor((steps / 100).toDouble())
            transaction.set(
                coinsRef,
                mapOf("coins" to FieldValue.increment(newCoins)), // 1 coin per 100 steps
                SetOptions.merge()
            )

            null
        }
            .addOnSuccessListener { println("Daily, total, and coins updated!")
            // Update Local DB
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // update daily
                        val id = "${uid}_$today"
                        val currentDaily = SyncManager.dailyDataDao().getDailyFlow(uid, today)
                        val dailySnap = userDocRef.collection("Health Data").document(today).get().await()
                        val totalSnap = userDocRef.collection("Health Data").document("Total").get().await()
                        if (dailySnap.exists()) {
                            val dsteps = (dailySnap.get("steps") as? Number)?.toLong() ?: 0L
                            val dmiles = (dailySnap.get("miles") as? Number)?.toDouble() ?: 0.0
                            val dcal = (dailySnap.get("calories") as? Number)?.toLong() ?: 0L
                            SyncManager.dailyDataDao().insert(
                                LocalDailyData(
                                    id = id,
                                    uid = uid,
                                    date = today,
                                    steps = dsteps,
                                    miles = dmiles,
                                    calories = dcal
                                )
                            )
                        }
                        // update totals
                        if (totalSnap.exists()) {
                            val tsteps = (totalSnap.get("steps") as? Number)?.toLong() ?: 0L
                            val tmiles = (totalSnap.get("miles") as? Number)?.toDouble() ?: 0.0
                            val tcal = (totalSnap.get("calories") as? Number)?.toLong() ?: 0L
                            val updatedAt = (totalSnap.get("updatedAt") as? com.google.firebase.Timestamp)?.seconds ?: System.currentTimeMillis() / 1000
                            SyncManager.userTotalsDao().insert(LocalUserTotals(uid = uid, steps = tsteps, miles = tmiles, calories = tcal, updatedAt = updatedAt))
                        }
                        // update coins
                        val coinsSnap = userDocRef.collection("Coins").document("Balance").get().await()
                        if (coinsSnap.exists()) {
                            val coins = (coinsSnap.get("Coins") as? Number)?.toLong() ?: 0L
                            SyncManager.coinsDao().insert(LocalCoinBalance(uid = uid, coins = coins))
                        }
                    } catch (e: Exception) {
                        Log.e("TrekFirebase", "Error updating local DB after logActivity: ${e.message}")
                    }
                }
            }
            .addOnFailureListener { e -> println("Error updating data: $e") }
    }

    /* Function: getUserTotals
     * Parameters:
     *      @onResult - throws error and success messages
     * Purpose: Collects the totals each user has since account creation
     *          could be use for account stats or leaderboards
     * Use Example:
     *      TrekFirebase.getUserTotals { steps, miles, calories ->
     *      if (steps >= 0) {
     *          totalStepsText.text = "Total Steps: $steps"
     *          totalMilesText.text = "Total Miles: $miles"
     *          totalCaloriesText.text = "Total Calories: $calories"
     *      } else {
     *          Toast.makeText(this, "Failed to load total data", Toast.LENGTH_SHORT).show()
     *      }
     *  }
     *
     */
    fun getUserTotals(onResult: (steps: Int, miles: Double, calories: Int) -> Unit) {
        val uid = getCurrentUserId() ?: return
        Log.d("DEBUG", "Fetching totals for user: $uid")

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val local = SyncManager.userTotalsDao().getTotalsFlow(uid)
                CoroutineScope(Dispatchers.IO).launch {
                    val db = SyncManager.userTotalsDao()
                    val maybe = db.getTotalsFlow(uid)
                }
            } catch (e: Exception) {
                // continue to listener
            }
        }
        FirebaseFirestore.getInstance()     //get user total document
            .collection("User Data")
            .document(uid)
            .collection("Health Data")
            .document("Total")
            .addSnapshotListener { document, error ->
                // Cannot access database
                if (error != null) {
                    Log.e("DEBUG", "Listen failed", error)
                    onResult(-1, -1.0, -1)
                    return@addSnapshotListener
                }
                // Found document and access data
                if (document != null && document.exists()) {
                    val steps = (document.get("steps") as? Number)?.toInt() ?: 0
                    val miles = (document.get("miles") as? Number)?.toDouble() ?: 0.0
                    val calories = (document.get("calories") as? Number)?.toInt() ?: 0
                    // update local DB
                    CoroutineScope(Dispatchers.IO).launch {
                        val updatedAt =
                            (document.get("updatedAt") as? com.google.firebase.Timestamp)?.seconds
                                ?: (System.currentTimeMillis() / 1000)
                        SyncManager.userTotalsDao().insert(LocalUserTotals(
                            uid = uid,
                            steps = steps.toLong(),
                            miles = miles,
                            calories = calories.toLong(),
                            updatedAt = updatedAt
                        ))
                    }
                    Log.d("FirestoreDebug", "Totals fetched: steps=$steps, miles=$miles, calories=$calories")
                    onResult(steps, miles, calories)
                }
                // Cannot find the data
                else {
                    Log.w("FirestoreDebug", "Total document does not exist for user $uid")

                    onResult(0, 0.0, 0)
                }
            }
    }

    /* Function: getDailyData
     * Parameters:
     *      @onResult - throws error and success messages
     * Purpose: Collects the daily total from the user, could be used for stat cards
     * Use Example:
     *      TrekFirebase.getDailyData { steps, miles, calories ->
     *      if (steps >= 0) {
     *          totalStepsText.text = "Total Steps: $steps"
     *          totalMilesText.text = "Total Miles: $miles"
     *          totalCaloriesText.text = "Total Calories: $calories"
     *      } else {
     *          Toast.makeText(this, "Failed to load total data", Toast.LENGTH_SHORT).show()
     *      }
     *  }
     *
     */
    fun getDailyData(date: String, onResult: (steps: Int, miles: Double, calories: Int) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()         // gettimg user daily data
            .collection("User Data")
            .document(uid)
            .collection("Health Data")
            .document(date)
            .addSnapshotListener { document, error ->
                // Cannot access database
                if (error != null) {
                    Log.e("DEBUG", "Listen failed", error)
                    onResult(-1, -1.0, -1)
                    return@addSnapshotListener
                }
                // Found document and data
                if (document != null && document.exists()) {
                    val steps = (document.get("steps") as? Number)?.toInt() ?: 0
                    val miles = (document.get("miles") as? Number)?.toDouble() ?: 0.0
                    val calories = (document.get("calories") as? Number)?.toInt() ?: 0
                    // update local DB
                    CoroutineScope(Dispatchers.IO).launch {
                        val id = "${uid}_$date"
                        SyncManager.dailyDataDao().insert(LocalDailyData(
                            id = id,
                            uid = uid,
                            date = date,
                            steps = steps.toLong(),
                            miles = miles,
                            calories = calories.toLong())
                        )
                    }
                    onResult(steps, miles, calories)
                }
                // No data
                else {
                    Log.w("FirestoreDebug", "Daily document does not exist for user $uid")

                    onResult(0, 0.0, 0)
                }
            }
    }
    /* Function: getUserFiles
     * Parameters:
     *      @userId: String - Can be collected through getUserID()
     * Purpose: Returns two lists of the file names a user has locked and has unlocked
     * Use Example:
     *      lifecycleScope.launch {
     *          val (locked, unlocked) = getUserFiles(userId)
     *      }
     */
    suspend fun getUserFiles(userId: String): Pair<List<String>, List<String>> {
        try {
            SyncManager.fetchUserFilesOnce(userId)
            val locked = SyncManager.avatarDao().getLockedFlow()
            val unlocked = SyncManager.avatarDao().getUnlockedFlow()

            val db = FirebaseFirestore.getInstance()

            val lockedRef = db.collection("User Data").document(userId).collection("locked")
            val unlockedRef = db.collection("User Data").document(userId).collection("unlocked")

            val lockedSnap = lockedRef.get().await()
            val unlockedSnap = unlockedRef.get().await()

            val lockedFiles = lockedSnap.documents.mapNotNull { it.getString("fileName") }
            val unlockedFiles = unlockedSnap.documents.mapNotNull { it.getString("fileName") }
            return Pair(lockedFiles, unlockedFiles)

        } catch (e: Exception) {
            return Pair(emptyList(), emptyList())
        }
    }

}

