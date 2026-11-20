package com.example.data.firebase

import android.content.Context
import com.example.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings

object FirestoreProvider {
    @Volatile
    private var instance: FirebaseFirestore? = null

    fun getDb(context: Context): FirebaseFirestore {
        return instance ?: synchronized(this) {
            instance ?: run {
                val appContext = context.applicationContext
                val dbId = appContext.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(dbId)
                try {
                    val settings = FirebaseFirestoreSettings.Builder()
                        .setLocalCacheSettings(
                            PersistentCacheSettings.newBuilder()
                                .setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                                .build()
                        )
                        .build()
                    db.firestoreSettings = settings
                } catch (_: IllegalStateException) {
                    // Settings already initialized on this instance
                }
                instance = db
                db
            }
        }
    }
}
