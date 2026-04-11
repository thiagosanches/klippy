# Proguard rules for Klippy

# Keep Bouncy Castle classes
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# Keep security crypto classes
-keep class androidx.security.crypto.** { *; }

# Keep data classes and models
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

# Standard Android rules
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exception
