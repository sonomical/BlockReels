# BlockReels release keep rules (R8)

# Native state & settings enums persisted via DataStore / JSON round-trips
-keep class com.blockreels.app.state.** { *; }
-keep class com.blockreels.app.detection.** { *; }
-keep class com.blockreels.app.disambiguation.** { *; }
-keep enum com.blockreels.app.data.** { *; }

# Kotlin coroutines internals safety net
-dontwarn kotlinx.coroutines.**

# Strip verbose and debug logging overhead from release bytecode
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}
