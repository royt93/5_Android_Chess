# AdMob SDK
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**

# Keep AdMobManager and related classes
-keep class com.saigonphantomlabs.sdkadbmob.** { *; }
-dontwarn com.saigonphantomlabs.sdkadbmob.**

# Keep ProductionConfig
-keep class com.saigonphantomlabs.sdkadbmob.ProductionConfig { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}