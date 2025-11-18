# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep line numbers for debugging
-keepattributes SourceFile,LineNumberTable

# Keep Bluetooth HID related classes
-keep class com.dps.droidpadmacos.bluetooth.** { *; }
-keep class android.bluetooth.** { *; }

# USB classes removed - app is Bluetooth only

# Keep utility classes
-keep class com.dps.droidpadmacos.util.** { *; }

# Keep ViewModel classes
-keep class com.dps.droidpadmacos.viewmodel.** { *; }

# Keep data classes
-keepclassmembers class com.dps.droidpadmacos.model.** {
    <fields>;
}

# Keep Compose related classes
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep Material3 components
-keep class com.google.android.material.** { *; }

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Keep lifecycle components
-keep class androidx.lifecycle.** { *; }

# Keep sensor related classes
-keep class com.dps.droidpadmacos.sensor.** { *; }

# Keep touchpad gesture detection
-keep class com.dps.droidpadmacos.touchpad.** { *; }

# Keep UI components
-keep class com.dps.droidpadmacos.ui.** { *; }

# Keep data management classes
-keep class com.dps.droidpadmacos.data.** { *; }

# Keep activities
-keep class com.dps.droidpadmacos.MainActivity { *; }
-keep class com.dps.droidpadmacos.FullScreenTrackpadActivity { *; }
-keep class com.dps.droidpadmacos.SplashActivity { *; }
-keep class com.dps.droidpadmacos.DiscoverableActivity { *; }
# UsbConnectionActivity removed - app is Bluetooth only
-keep class com.dps.droidpadmacos.SettingsActivity { *; }
-keep class com.dps.droidpadmacos.HelpActivity { *; }

# Remove logging in release builds - Critical for production
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Remove debug logging from custom Logger in release builds
-assumenosideeffects class com.dps.droidpadmacos.util.Logger {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** wtf(...);
}

# Keep R class
-keepclassmembers class **.R$* {
    public static <fields>;
}

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# Keep Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}