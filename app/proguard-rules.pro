# R8 rules — Media3 & kotlinx-serialization
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# kotlinx-serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class id.secretarrow.alquran.**$$serializer { *; }
-keepclassmembers class id.secretarrow.alquran.** { *** Companion; }
-keepclasseswithmembers class id.secretarrow.alquran.** { kotlinx.serialization.KSerializer serializer(...); }

# Adhan
-keep class com.batoulapps.adhan.** { *; }
-dontwarn com.batoulapps.adhan.**
