import com.android.build.api.variant.VariantFilter
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.simple.rmg.RoomMetadataGenerator

repositories {
  maven(url = "https://jitpack.io")
}

plugins {
  id("com.android.application")
  kotlin("android")
  kotlin("kapt")
  id("kotlin-parcelize")
  id("io.sentry.android.gradle")
  id("com.google.firebase.firebase-perf")
  id("plugins.git.install-hooks")
}

sentry {
  autoProguardConfig = false
  // We can disable auto upload of proguard mappings since we use -dontobfuscate when running proguard
  autoUpload = false
}

kapt {
  arguments {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", false)
    arg("room.expandProjection", false)
  }
}

tasks.withType<Test> {
  testLogging {
    // set options for log level LIFECYCLE
    events = setOf(
        TestLogEvent.PASSED,
        TestLogEvent.SKIPPED,
        TestLogEvent.FAILED,
        TestLogEvent.STANDARD_OUT
    )
    showExceptions = true
    exceptionFormat = TestExceptionFormat.FULL
    showCauses = true
    showStackTraces = true

    // set options for log level DEBUG and INFO
    debug {
      events = setOf(
          TestLogEvent.STARTED,
          TestLogEvent.PASSED,
          TestLogEvent.SKIPPED,
          TestLogEvent.FAILED,
          TestLogEvent.STANDARD_OUT,
          TestLogEvent.STANDARD_ERROR
      )
    }
    info.events = debug.events

    addTestListener(object : TestListener {
      override fun beforeSuite(descriptor: TestDescriptor?) {}
      override fun afterSuite(descriptor: TestDescriptor?, result: TestResult?) {}
      override fun beforeTest(descriptor: TestDescriptor?) {}
      override fun afterTest(descriptor: TestDescriptor?, result: TestResult?) {
        if (descriptor?.parent != null && result != null) { // will match the outermost suite
          val output = "Results: ${result.resultType} (${result.testCount} tests, ${result.successfulTestCount} successes, ${result.failedTestCount} failures, ${result.skippedTestCount} skipped)"
          val startItem = "|  "
          val endItem = "  |"
          val repeatLength = startItem.length + output.length + endItem.length
          println("\n" + ("-".repeat(repeatLength)) + "\n" + startItem + output + endItem + "\n" + ("-".repeat(repeatLength)))
        }
      }
    })
  }
}

android {
  val compileSdk: Int by rootProject.extra
  val minSdk: Int by rootProject.extra
  val targetSdk: Int by rootProject.extra

  compileSdkVersion(compileSdk)
  // Needed to switch NDK versions on the CI server since they have different
  // NDK versions on macOS and Linux environments. Gradle plugin 3.6+ requires
  // us to pin an NDK version if we package native libs.
  // https://developer.android.com/studio/releases/gradle-plugin#default-ndk-version
  //
  // Currently, this is only used for assembling the APK where the build process
  // strips debug symbols from the APK.
  val androidNdkVersion: String by project
  ndkVersion = androidNdkVersion

  defaultConfig {
    applicationId = "org.simple.clinic"
    minSdkVersion(minSdk)
    targetSdkVersion(targetSdk)
    versionCode = 1
    versionName = "0.1"
    multiDexEnabled = true

    testInstrumentationRunner = "org.simple.clinic.AndroidTestJUnitRunner"

    val defaultProguardFile: String by project
    proguardFiles(
        getDefaultProguardFile(defaultProguardFile),
        "proguard-rules.pro"
    )

    vectorDrawables.useSupportLibrary = true

    val sentryDsn: String by project
    val sentryEnvironment: String by project

    addManifestPlaceholders(mapOf(
        "sentryDsn" to sentryDsn,
        "sentryEnvironment" to sentryEnvironment
    ))
  }

  buildTypes {
    getByName("debug") {
      applicationIdSuffix = ".debug"
      isMinifyEnabled = false
      isShrinkResources = false
    }

    val runProguard: String by project
    getByName("release") {
      isDebuggable = false
      isMinifyEnabled = runProguard.toBoolean()
      isShrinkResources = runProguard.toBoolean()
    }
  }

  buildFeatures {
    viewBinding = true
  }

  flavorDimensions("track")

  productFlavors {
    create("qa") {
      val qaManifestEndpoint: String by project
      val qaFallbackApiEndpoint: String by project
      val qaMixpanelToken: String by project

      dimension = "track"
      applicationIdSuffix = ".qa"
      versionNameSuffix = "-qa"
      buildConfigField("String", "MANIFEST_ENDPOINT", "\"$qaManifestEndpoint\"")
      buildConfigField("String", "FALLBACK_ENDPOINT", "\"$qaFallbackApiEndpoint\"")
      buildConfigField("String", "MIXPANEL_TOKEN", "\"$qaMixpanelToken\"")
      buildConfigField("boolean", "DISABLE_SCREENSHOT", "false")
      buildConfigField("boolean", "ALLOW_ROOTED_DEVICE", "true")
    }

    create("staging") {
      val stagingManifestEndpoint: String by project
      val stagingFallbackApiEndpoint: String by project
      val stagingMixpanelToken: String by project

      dimension = "track"
      applicationIdSuffix = ".staging"
      versionNameSuffix = "-demo"
      buildConfigField("String", "MANIFEST_ENDPOINT", "\"$stagingManifestEndpoint\"")
      buildConfigField("String", "FALLBACK_ENDPOINT", "\"$stagingFallbackApiEndpoint\"")
      buildConfigField("String", "MIXPANEL_TOKEN", "\"$stagingMixpanelToken\"")
      buildConfigField("boolean", "DISABLE_SCREENSHOT", "false")
      buildConfigField("boolean", "ALLOW_ROOTED_DEVICE", "true")
    }

    create("sandbox") {
      val sandboxManifestEndpoint: String by project
      val sandboxFallbackApiEndpoint: String by project
      val sandboxMixpanelToken: String by project

      dimension = "track"
      applicationIdSuffix = ".sandbox"
      versionNameSuffix = "-sandbox"
      buildConfigField("String", "MANIFEST_ENDPOINT", "\"$sandboxManifestEndpoint\"")
      buildConfigField("String", "FALLBACK_ENDPOINT", "\"$sandboxFallbackApiEndpoint\"")
      buildConfigField("String", "MIXPANEL_TOKEN", "\"$sandboxMixpanelToken\"")
      buildConfigField("boolean", "DISABLE_SCREENSHOT", "false")
      buildConfigField("boolean", "ALLOW_ROOTED_DEVICE", "true")
    }

    create("security") {
      val securityManifestEndpoint: String by project
      val securityFallbackApiEndpoint: String by project
      val securityMixpanelToken: String by project

      dimension = "track"
      applicationIdSuffix = ".security"
      versionNameSuffix = "-security"
      buildConfigField("String", "MANIFEST_ENDPOINT", "\"$securityManifestEndpoint\"")
      buildConfigField("String", "FALLBACK_ENDPOINT", "\"$securityFallbackApiEndpoint\"")
      buildConfigField("String", "MIXPANEL_TOKEN", "\"$securityMixpanelToken\"")
      buildConfigField("boolean", "DISABLE_SCREENSHOT", "true")
      buildConfigField("boolean", "ALLOW_ROOTED_DEVICE", "false")
    }

    create("production") {
      val productionManifestEndpoint: String by project
      val productionFallbackApiEndpoint: String by project
      val productionMixpanelToken: String by project

      dimension = "track"
      buildConfigField("String", "MANIFEST_ENDPOINT", "\"$productionManifestEndpoint\"")
      buildConfigField("String", "FALLBACK_ENDPOINT", "\"$productionFallbackApiEndpoint\"")
      buildConfigField("String", "MIXPANEL_TOKEN", "\"$productionMixpanelToken\"")
      buildConfigField("boolean", "DISABLE_SCREENSHOT", "true")
      buildConfigField("boolean", "ALLOW_ROOTED_DEVICE", "false")
    }
  }

  val filteredVariants = setOf(
      "qaRelease", "stagingDebug", "sandboxDebug", "productionDebug", "securityDebug"
  )
  variantFilter = Action<VariantFilter> {
    if (name in filteredVariants) {
      ignore = true
    }
  }

  lintOptions {
    isWarningsAsErrors = true
    isAbortOnError = true
    isCheckReleaseBuilds = false
  }

  compileOptions {
    isCoreLibraryDesugaringEnabled = true
  }

  kotlinOptions {
    jvmTarget = "1.8"
    freeCompilerArgs = freeCompilerArgs + listOf("-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi")
  }

  sourceSets {
    val sharedTestDir = "src/sharedTest/java"
    getByName("test") {
      java.srcDirs(sharedTestDir)
    }

    getByName("androidTest") {
      assets.srcDirs(files("$projectDir/schemas"))
      java.srcDirs(sharedTestDir)
    }

    getByName("main") {
      assets.srcDirs("${project.buildDir}/generated/assets/room_dao_metadata/")
    }
  }

  packagingOptions {
    // Deprecated ABIs. See https://developer.android.com/ndk/guides/abis
    exclude("lib/mips/libsqlite3x.so")
    exclude("lib/mips64/libsqlite3x.so")
    exclude("lib/armeabi/libsqlite3x.so")
  }

  bundle {
    language {
      // Specifies that the app bundle should not support configuration APKs for language resources.
      // These resources are instead packaged with each base and dynamic feature APK.
      // We are doing this since we support switching the language within the app and we want all
      // translations to be delivered to all devices.
      enableSplit = false
    }
  }

  // We don"t obfuscate (only minify) using proguard. Gradle plugin 3.2.0 (and greater?) generates
  // an empty mappings.txt file. This caused an issue where the CI deploy to play store task tries
  // to upload the empty mapping file, which causes the Play Store api to complain.
  val deleteProguardMappings = tasks.create<Delete>("deleteProguardMappings") {
    delete(fileTree(buildDir).matching {
      include("outputs/mapping/**/mapping.txt")
    })
  }

  afterEvaluate {
    val assembleReleaseTasks = setOf(
        "assembleStagingRelease",
        "assembleSandboxRelease",
        "assembleProductionRelease",
        "assembleSecurityRelease"
    )

    assembleReleaseTasks.forEach { buildType ->
      tasks.findByName(buildType)?.finalizedBy(deleteProguardMappings)
    }

    val kaptTasks = mapOf(
        "kaptQaDebugKotlin" to "qaDebug",
        "kaptSandboxReleaseKotlin" to "sandboxRelease",
        "kaptStagingReleaseKotlin" to "stagingRelease",
        "kaptSecurityReleaseKotlin" to "securityRelease",
        "kaptProductionReleaseKotlin" to "productionRelease"
    )

    kaptTasks.forEach { (buildType, sourceSetName) ->
      val taskQualifier = buildType
          .replace("kapt", "")
          .replace("Kotlin", "")

      val taskName = "transform${taskQualifier}GeneratedRoomDao"
      val transformRoomDaoTask = tasks.create<TransformGeneratedRoomDaoTask>(taskName) {
        sourceSet.set(sourceSetName)
        reporterClassName.set("org.simple.clinic.storage.monitoring.SqlPerformanceReporter")
      }

      tasks.findByName(buildType)?.finalizedBy(transformRoomDaoTask)
      tasks.named("compile${taskQualifier}JavaWithJavac").configure {
        dependsOn(transformRoomDaoTask)
      }
    }
  }
}

dependencies {
  coreLibraryDesugaring(libs.desugaring)

  implementation(projects.mobiusBase)
  implementation(projects.router)
  implementation(projects.simplePlatform)
  implementation(projects.simpleVisuals)

  lintChecks(projects.lint)

  /**
   * Debug dependencies
   */
  debugImplementation(libs.faker)

  debugImplementation(libs.flipper.flipper)
  debugImplementation(libs.flipper.networkPlugin)

  debugImplementation(libs.leakcanary)

  debugImplementation(libs.soloader)

  /**
   * Prod dependencies
   */
  implementation(libs.androidx.annotation.annotation)
  implementation(libs.androidx.annotation.experimental)
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.cardview)
  implementation(libs.androidx.constraintlayout)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.fragment)
  implementation(libs.androidx.recyclerview)
  implementation(libs.androidx.viewpager2)

  implementation(libs.bundles.androidx.camera)

  implementation(libs.bundles.androidx.paging)

  implementation(libs.bundles.androidx.room)
  kapt(libs.androidx.room.compiler)

  implementation(libs.bundles.androidx.work)

  implementation(libs.bundles.moshi)
  kapt(libs.moshi.codegen)

  implementation(libs.bundles.okhttp)

  implementation(libs.bundles.retrofit)

  implementation(libs.bundles.rx.binding)

  implementation(libs.dagger.dagger)
  kapt(libs.dagger.compiler)

  implementation(libs.edittext.masked)
  implementation(libs.edittext.pinentry)

  implementation(libs.firebase.config)
  implementation(libs.firebase.performance.perf)

  implementation(libs.flow)

  implementation(libs.itemanimators)

  runtimeOnly(libs.jackson.core)

  implementation(libs.jbcrypt)

  implementation(libs.kotlin.coroutines)
  implementation(libs.kotlin.stdlib)

  implementation(libs.logback.classic)

  implementation(libs.lottie)

  implementation(libs.material)

  implementation(libs.mixpanel.android)

  implementation(libs.okhttp.interceptor.logging)

  implementation(libs.play.core)
  implementation(libs.play.services.auth)
  implementation(libs.play.services.location)
  implementation(libs.play.services.mlkit.barcode)

  implementation(libs.rootbeer)

  implementation(libs.rx.android)
  implementation(libs.rx.java)
  implementation(libs.rx.kotlin)
  implementation(libs.rx.preferences)

  implementation(libs.sentry.android) {
    exclude(group = "com.fasterxml.jackson.core", module = "jackson-core")
  }

  implementation(libs.signaturepad)

  implementation(libs.sqlite.android)

  implementation(libs.threeten.extra)

  implementation(libs.traceur)

  implementation(libs.uuid.generator)

  implementation(libs.viewpump)

  implementation(libs.zxing)

  /**
   * Unit test dependencies
   */
  testRuntimeOnly(libs.asm) {
    because("not mandatory, but Truth recommends adding this dependency for better error reporting")
  }

  testImplementation(projects.mobiusMigration)

  testImplementation(libs.androidx.paging.common)

  testImplementation(libs.mobius.test)

  testImplementation(libs.junit)
  testImplementation(libs.junitParams)
  testImplementation(libs.mockito.kotlin)
  testImplementation(libs.truth)
  testImplementation(libs.faker)
  testImplementation(libs.quarantine)

  /**
   * Android test dependencies
   */
  androidTestImplementation(libs.androidx.annotation.annotation)
  androidTestImplementation(libs.androidx.archCoreTesting) {
    // This dependency transitively pulls in a newer version of Mockito than Mockito-Kotlin does.
    // This results in the import statements in the unit tests breaking for mockito methods because
    // the IDE uses the version of Mockito that is present in the classpath from the androidTest
    // sourceset dependency, which raises an inspection error in the IDE. We can explicitly exclude
    // it from this dependency because it is also pulled in via mockito-kotlin.
    exclude(group = "org.mockito", module = "mockito-core")
  }
  androidTestImplementation(libs.androidx.room.testing)

  androidTestImplementation(libs.bundles.androidx.test)

  androidTestRuntimeOnly(libs.asm) {
    because("not mandatory, but Truth recommends adding this dependency for better error reporting")
  }

  androidTestImplementation(libs.faker)

  androidTestImplementation(libs.quarantine) {
    exclude(group = "com.fasterxml.jackson.core", module = "jackson-core")
  }

  androidTestImplementation(libs.truth)

  kaptAndroidTest(libs.dagger.compiler)
}

// This must always be present at the bottom of this file, as per:
// https://console.firebase.google.com/u/2/project/simple-org/settings/general/
apply(plugin = "com.google.gms.google-services")

abstract class TransformGeneratedRoomDaoTask : DefaultTask() {

  @get:Input
  abstract val sourceSet: Property<String>

  @get:Input
  abstract val reporterClassName: Property<String>

  @TaskAction
  fun run() {
    val rmg = RoomMetadataGenerator()

    rmg.run(project.projectDir.absolutePath, sourceSet.get(), reporterClassName.get())
  }
}
