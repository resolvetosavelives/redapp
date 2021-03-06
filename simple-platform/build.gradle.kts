plugins {
  id("com.android.library")
  kotlin("android")
}

android {
  compileSdkVersion(versions.compileSdk)

  defaultConfig {
    minSdkVersion(versions.minSdk)
    targetSdkVersion(versions.compileSdk)
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
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:${versions.kotlin}")
  api("com.jakewharton.timber:timber:${versions.timber}")
}
