# Keep domain models serialized by Firebase Firestore
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.example.domain.** { *; }
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.DocumentId <fields>;
    @com.google.firebase.firestore.ServerTimestamp <fields>;
}

# Room & KSP
-keep class * extends androidx.room.RoomDatabase
-keep class com.example.data.local.** { *; }

# Keep Firebase App Check & Credential Manager
-keep class com.google.firebase.appcheck.** { *; }
-keep class androidx.credentials.** { *; }
