package com.example.trekfirebase

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object TrekFirebase {
    private fun auth(): FirebaseAuth = FirebaseAuth.getInstance()
    private fun db(): FirebaseFirestore = FirebaseFirestore.getInstance()

    // Below deals with user accounts
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
                        "UserUID" to uid
                    )
                    // Starts a total document, to collect a cumulation of all health data
                    userDoc.set(initialData)
                        .addOnSuccessListener {
                            val healthDataRef = userDoc.collection("Health Data")
                            // Inititalize to zero
                            val totalDoc = hashMapOf(
                                "steps" to 0,
                                "miles" to 0.0,
                                "calories" to 0
                            )
                            healthDataRef.document("Total").set(totalDoc)
                                .addOnSuccessListener {
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
    fun logActivity(steps: Long, miles: Double, calories: Long) {
        val db = db()
        val uid = getCurrentUserId() ?: return
        val userDocRef = db.collection("User Data").document(uid)
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val dailyRef = userDocRef.collection("Health Data").document(today)
        val totalRef = userDocRef.collection("Health Data").document("Total")

        db.runTransaction { transaction ->

            // --- READ DAILY FIRST ---
            val dailySnap = transaction.get(dailyRef)
            val updatedDailySteps = (dailySnap.getLong("steps") ?: 0L) + steps
            val updatedDailyMiles = (dailySnap.getDouble("miles") ?: 0.0) + miles
            val updatedDailyCalories = (dailySnap.getLong("calories") ?: 0L) + calories


            // --- READ TOTAL AFTER DAILY ---
            val totalSnap = transaction.get(totalRef)
            val updatedTotalSteps = (totalSnap.getLong("steps") ?: 0L) + steps
            val updatedTotalMiles = (totalSnap.getDouble("miles") ?: 0.0) + miles
            val updatedTotalCalories = (totalSnap.getLong("calories") ?: 0L) + calories

            // --- WRITE TOTAL ---
            transaction.set(
                totalRef, mapOf(
                    "steps" to updatedTotalSteps,
                    "miles" to updatedTotalMiles,
                    "calories" to updatedTotalCalories
                )
            )

            // --- WRITE DAILY ---
            transaction.set(
                dailyRef, mapOf(
                    "steps" to updatedDailySteps,
                    "miles" to updatedDailyMiles,
                    "calories" to updatedDailyCalories,
                    "date" to today
                )
            )

            null
        }
            .addOnSuccessListener { println("Daily & total updated!") }
            .addOnFailureListener { e -> println("Error updating data: $e") }
    }


    fun getUserTotals(onResult: (steps: Int, miles: Double, calories: Int) -> Unit) {
        val uid = getCurrentUserId() ?: return
        Log.d("DEBUG", "Fetching totals for user: $uid")

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
                    onResult(steps, miles, calories)
                }
                // No data
                else {
                    Log.w("FirestoreDebug", "Daily document does not exist for user $uid")

                    onResult(0, 0.0, 0)
                }
            }
    }
}

