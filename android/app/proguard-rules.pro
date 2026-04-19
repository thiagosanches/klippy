# Proguard rules for Klippy

# Keep all Bouncy Castle classes (comprehensive)
-keep class org.bouncycastle.** { *; }
-keepclassmembers class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**
-dontnote org.bouncycastle.**

# Keep Bouncy Castle JCE provider
-keep class org.bouncycastle.jce.provider.BouncyCastleProvider { *; }
-keep class org.bouncycastle.jcajce.provider.** { *; }

# Keep PGP classes specifically
-keep class org.bouncycastle.openpgp.** { *; }
-keep class org.bouncycastle.bcpg.** { *; }

# Keep ASN1 classes (these were missing and causing the crash)
-keep class org.bouncycastle.asn1.** { *; }

# Keep security crypto classes
-keep class androidx.security.crypto.** { *; }

# Keep data classes and models
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exception
