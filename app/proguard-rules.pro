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

# WorkManager (pulled in by Google Mobile Ads) is started by androidx.startup at process
# launch and opens its Room database. Room instantiates the generated "<Db>_Impl" class
# reflectively, so R8 must keep those classes and their no-arg constructors; without this
# the release build crashes at launch with
# "Failed to create an instance of androidx.work.impl.WorkDatabase".
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep class androidx.work.impl.WorkDatabase_Impl { *; }
-keep class androidx.work.impl.WorkDatabase { *; }
-keep class * extends androidx.startup.Initializer { <init>(); }
-keep class androidx.work.WorkManagerInitializer { *; }
