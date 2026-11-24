# BlockReels release keep rules (R8)

# Native state persisted via DataStore / JSON round-trips
-keep class com.blockreels.app.state.** { *; }
-keep class com.blockreels.app.detection.** { *; }
-keep class com.blockreels.app.disambiguation.** { *; }

# Kotlin coroutines internals safety net
-dontwarn kotlinx.coroutines.**
