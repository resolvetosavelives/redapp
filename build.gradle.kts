import com.diffplug.spotless.LineEnding.UNIX

buildscript {
  extra.apply {
    set("compileSdk", 30)
    set("minSdk", 21)
    set("targetSdk", 30)
  }

  repositories {
    google()
    mavenCentral()
    maven(url = "https://jitpack.io")
  }

  dependencies {
    val libs = project.extensions.getByType<VersionCatalogsExtension>().named("libs") as org.gradle.accessors.dm.LibrariesForLibs
    val roomMetaDataGeneratorVersion = libs.versions.room.metadataGenerator.get()

    classpath(libs.android.gradle.plugin)
    classpath(libs.kotlin.gradle.plugin)
    classpath(libs.sentry.gradle.plugin)
    classpath(libs.google.services)
    classpath(libs.firebase.performance.plugin)
    classpath(files("./buildTooling/room-metadata-generator-$roomMetaDataGeneratorVersion.jar"))
  }
}

plugins {
  id("com.diffplug.spotless") version versions.spotless
}

allprojects {
  repositories {
    google()
    mavenCentral()

    // Left to support migration away from JCenter, do not add any new dependency inclusions here.
    // See: https://jeroenmols.com/blog/2021/02/04/migratingjcenter/
    jcenter {
      content {
        // :app
        includeModule("com.facebook.flipper", "flipper")
        includeModule("com.facebook.flipper", "flipper-network-plugin")
        includeModule("ru.egslava", "MaskedEditText")
      }
    }
  }
}

subprojects {
  apply(plugin = "com.diffplug.spotless")

  // https://github.com/diffplug/spotless/tree/master/plugin-gradle
  spotless {
    lineEndings = UNIX

    kotlin {
      ktlint(libs.versions.ktlint.get()).userData(mapOf(
          "indent_style" to "space",
          "indent_size" to "2",
          "continuation_indent_size" to "4"
      ))

      trimTrailingWhitespace()
      endWithNewline()
    }
  }
}

tasks.named<Delete>("clean") {
  delete(rootProject.buildDir)
}
