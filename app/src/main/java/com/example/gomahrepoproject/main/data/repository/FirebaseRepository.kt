package com.example.gomahrepoproject.main.data.repository

import com.google.firebase.firestore.FirebaseFirestore

class FirebaseRepository {

    private val db = FirebaseFirestore.getInstance()

    fun getBlockedApps(userId: String, childId: String, onResult: (List<String>) -> Unit) {
        db.collection("users").document(userId)
            .collection("children").document(childId)
            .get()
            .addOnSuccessListener { document ->
                val blockedApps = document.get("blockedApps") as? List<String> ?: emptyList()
                onResult(blockedApps)
            }
            .addOnFailureListener {
                onResult(emptyList()) // Handle failure
            }
    }
}
