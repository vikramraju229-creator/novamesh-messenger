# NovaMesh Messenger ProGuard / R8 Rules

# ─── Kotlin ───
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.novamesh.**$$serializer { *; }
-keepclassmembers class com.novamesh.** {
    *** Companion;
}
-keepclasseswithmembers class com.novamesh.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ─── Compose ───
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ─── Room ───
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ─── SQLCipher ───
-keep class net.zetetic.** { *; }
-dontwarn net.zetetic.**

# ─── Matrix SDK ───
-keep class org.matrix.** { *; }
-dontwarn org.matrix.**
-dontnote org.matrix.**

# ─── WebRTC ───
-keep class org.webrtc.** { *; }
-dontwarn org.webrtc.**

# ─── ML Kit ───
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# ─── Retrofit / OkHttp ───
-keepattributes Signature
-keepattributes Exceptions
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keepclassmembers class * {
    @retrofit2.http.* <methods>;
}
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontnote okhttp3.**

# ─── Gson ───
-keep class com.google.gson.** { *; }
-keepattributes *Annotation*, Signature
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ─── Firebase ───
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# ─── Coil ───
-keep class coil.** { *; }
-dontwarn coil.**

# ─── Media3 / ExoPlayer ───
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ─── ZXing ───
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# ─── Biometric ───
-keep class androidx.biometric.** { *; }

# ─── App specific models (keep for serialization) ───
-keep class com.novamesh.domain.model.** { *; }
-keepclassmembers class com.novamesh.domain.model.** { *; }

# ─── Keep data classes for Gson deserialization ───
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ─── General Android ───
-keep class * extends android.app.Application
-keep class * extends android.app.Service
-keep class * extends android.content.BroadcastReceiver
-keep class * extends android.content.ContentProvider
-keep class * extends android.app.Fragment
-keep class * extends androidx.fragment.app.Fragment

# ─── Keep BuildConfig ───
-keep class com.novamesh.BuildConfig { *; }

# ─── Serialization ───
# Keep `serializer()` for serializable classes
-keepclassmembers class kotlinx.serialization.Serializable {
    *** Companion;
}

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
