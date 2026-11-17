# Keep the gallery widget and its contents
-keep class com.nowlsantos.aiconcierge.ui.component.widget.** { *; }

# Keep resources used by getIdentifier dynamically in LoadImage
-keepclassmembers class **.R$* {
    public static <fields>;
}

# Keep common library classes if used dynamically
-keep class com.nowlsantos.common.** { *; }

# ==============================================================================
# CRITICAL: Prevent R8 shrinking, obfuscation, and optimization on OkHttp, Ktor,
# kotlinx.serialization, and Coroutines to ensure Gemini Live WebSocket frames
# never fail in release builds.
# ==============================================================================
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod, SourceFile, LineNumberTable

-keep class io.ktor.** { *; }
-keep class kotlinx.serialization.** { *; }
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

-dontwarn io.ktor.**
-dontwarn kotlinx.serialization.**
-dontwarn okhttp3.**
-dontwarn okio.**

# Prevent shrinking internal coroutine state machines used by WebSocket flows
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**
