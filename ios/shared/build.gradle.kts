import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
  kotlin("multiplatform")
  kotlin("plugin.compose")
  id("org.jetbrains.compose")
}

// Android-only files replaced on iOS by the versions in src/iosMain/kotlin.
val androidOnly = listOf(
  "com/example/MainActivity.kt",
  "com/example/ads/AdManager.kt",
  "com/example/ads/ConsentManager.kt",
  "com/example/ui/IntroScreen.kt",
)

kotlin {
  listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
    target.binaries.framework {
      baseName = "ArmaShared"
      isStatic = true
    }
  }

  sourceSets {
    iosMain {
      // The game itself: same files as the Android app, compiled unchanged.
      kotlin.srcDir("../../app/src/main/java")
      kotlin.exclude(androidOnly)
      dependencies {
        implementation(compose.runtime)
        implementation(compose.foundation)
        implementation(compose.material3)
        implementation(compose.materialIconsExtended)
        implementation(compose.ui)
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
      }
    }
  }

  compilerOptions {
    freeCompilerArgs.addAll("-Xexpect-actual-classes", "-opt-in=kotlinx.cinterop.ExperimentalForeignApi", "-opt-in=kotlinx.cinterop.BetaInteropApi")
  }
}
