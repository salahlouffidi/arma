// iOS build of ARMASNAKE. Separate from the Android build (../settings.gradle.kts),
// which is not touched: the shared Kotlin sources are read from ../app/src/main/java.
pluginManagement {
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
  }
}
dependencyResolutionManagement {
  repositories {
    google()
    mavenCentral()
  }
}
rootProject.name = "ArmaSnakeIos"
include(":shared")
