# exp4j
-keep class net.objecthunter.exp4j.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.**

# Timber
-keep class timber.log.** { *; }
