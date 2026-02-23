# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep Room entities and DAOs
-keep class com.medchecktag.models.** { *; }
-keep class com.medchecktag.database.** { *; }

# Keep Room annotations
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keep @androidx.room.Database class *

# Keep NFC classes
-keep class com.medchecktag.nfc.** { *; }

# Keep repository interfaces
-keep interface com.medchecktag.repositories.** { *; }

# Keep service interfaces
-keep interface com.medchecktag.alarms.IAlarmScheduler { *; }
-keep interface com.medchecktag.audio.IAudioFeedbackService { *; }
-keep interface com.medchecktag.nfc.INFCHandler { *; }

# Keep services (MissedDoseWorker, EmergencyNotificationService)
-keep class com.medchecktag.services.** { *; }

# Keep BroadcastReceivers
-keep class * extends android.content.BroadcastReceiver { *; }

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

# Keep ViewModels
-keep class * extends androidx.lifecycle.ViewModel { *; }

# AndroidX Lifecycle
-keep class androidx.lifecycle.** { *; }
-dontwarn androidx.lifecycle.**

# Room Database
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep WorkManager
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**

# General Android optimizations
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# Keep line numbers for debugging stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
