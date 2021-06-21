plugins {
  id("com.android.library")
  kotlin("android")
  kotlin("kapt")
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
    versionName = "1.0"
  }

  buildTypes {
    getByName("release") {
      minifyEnabled(false)
    }
  }
}

dependencies {
  implementation(libs.androidx.appcompat)

  implementation(libs.flow)

  implementation(libs.kotlin.stdlib)

  implementation(libs.rx.java)

  implementation(libs.timber)

  testImplementation(libs.junit)
  testImplementation(libs.junitParams)

  testImplementation(libs.truth)
}
