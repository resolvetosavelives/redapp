plugins {
  id("com.android.lint")
  kotlin("jvm")
}

dependencies {
  compileOnly(libs.kotlin.stdlib)

  // Lint
  compileOnly(libs.bundles.lint)

  // Testing
  testImplementation(libs.lint.tests)

  testImplementation(libs.junit)
}
