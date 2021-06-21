plugins {
  id("com.android.library")
  kotlin("android")
}

android {
  val compileSdk: Int by rootProject.extra
  val minSdk: Int by rootProject.extra
  val targetSdk: Int by rootProject.extra

  compileSdkVersion(compileSdk)

  defaultConfig {
    minSdkVersion(minSdk)
    targetSdkVersion(targetSdk)
    versionCode = 1
    versionName = "0.1"

    consumerProguardFiles("consumer-rules.pro")
  }

  buildTypes {
    getByName("release") {
      minifyEnabled(false)
      proguardFiles(
          getDefaultProguardFile("proguard-android-optimize.txt"),
          "proguard-rules.pro"
      )
    }
  }
}

dependencies {
  implementation(projects.simplePlatform)

  implementation(libs.kotlin.stdlib)

  api(libs.bundles.mobius)
}
