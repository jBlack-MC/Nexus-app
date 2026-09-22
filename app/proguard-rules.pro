# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keep @retrofit2.http.* interface * { *; }

# Gson
-keep class com.google.gson.** { *; }
-keep class com.example.nexus.api.** { *; }
-keepattributes *Annotation*
-dontwarn sun.misc.**

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
