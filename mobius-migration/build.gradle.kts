plugins {
  kotlin("jvm")
}

dependencies {
  implementation(libs.guava)

  implementation(libs.kotlin.stdlib)

  implementation(libs.mobius.core)
  implementation(libs.mobius.rx2)

  testImplementation(libs.junit)

  testImplementation(libs.truth)
}
