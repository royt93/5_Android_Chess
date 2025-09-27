# Aggressive APK size optimization rules

# Enable all optimizations
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# Remove unused code more aggressively
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# Remove debug code
-assumenosideeffects class java.io.PrintStream {
    public void println(%);
    public void println(**);
}

# Keep only necessary chess game classes
-keep public class com.saigonphantomlabs.** { public *; }
-keep public class com.saigonphantomlabs.chess.** { public *; }

# Glide optimization - minimal keeps for GIF support
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
    <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
    *** rewind();
}

# Keep GIF decoder specifically
-keep class com.bumptech.glide.load.resource.gif.** { *; }
-keep class com.bumptech.glide.gifdecoder.** { *; }

# More aggressive resource removal
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# Remove reflection calls that increase size
-assumenosideeffects class java.lang.Class {
    public java.lang.reflect.Method[] getDeclaredMethods();
    public java.lang.reflect.Field[] getDeclaredFields();
}

# Remove unused AndroidX features
-dontwarn androidx.lifecycle.**
-dontwarn androidx.savedstate.**

# Optimize native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Remove unused enum values
-optimizations !class/unboxing/enum