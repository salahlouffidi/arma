# ARMASNAKE — R8 rules for the release build.
#
# The app has no WebView JavaScript bridge, no JNI code of its own and no reflection-based
# serialisation of app classes, so no app-wide -keep rules are needed. AndroidX, Compose,
# Google Mobile Ads, UMP, Firebase, Moshi, Retrofit and OkHttp ship their own consumer
# rules inside their AARs/JARs. Do NOT add broad rules like "-keep class com.example.** { *; }"
# or "-dontobfuscate": they would defeat obfuscation (Play's DEX obfuscation warning).

# Keep line numbers so Play Console / Crashlytics stack traces are de-obfuscated precisely
# with mapping.txt, while hiding the original source file names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Generic signatures / annotations are needed by Moshi & Retrofit adapters if they are used.
-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod
