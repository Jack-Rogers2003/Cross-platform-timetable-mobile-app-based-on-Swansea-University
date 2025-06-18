package com.example.mobileapp

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.getInstance
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await
import com.google.firebase.database.FirebaseDatabase

object UserInformation{
    private var mAuth = getInstance()
    private val database = FirebaseDatabase.getInstance("https://dissertationapp-860fa-default-rtdb.europe-west1.firebasedatabase.app/")

    fun getAuth(): FirebaseAuth {
        return mAuth
    }

    fun logOut() {
        mAuth.signOut()
    }

    fun getCurrentUser() : FirebaseUser? {
        return mAuth.currentUser
    }

    fun getCurrentUserID(): String? {
        return getCurrentUser()?.uid
    }

    fun getDatabase(): FirebaseDatabase {
        return database
    }

    suspend fun getUsername(): String? {
        return try {
            val userId = getCurrentUserID()
            if (userId != null) {
                val snapshot = database.reference.child(userId).child("Username").get().await()
                snapshot.getValue(String::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun setDatabase(property: String, toSet: String) {
        getCurrentUserID()?.let {
            database.getReference(it).child(property).setValue(toSet).addOnCompleteListener{

            }
        }
    }
}