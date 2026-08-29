# Keep Supabase / OkHttp / kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable <fields>;
}
