enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
enableFeaturePreview("VERSION_CATALOGS")

pluginManagement {
  plugins {
    id("com.diffplug.spotless") version "5.6.1"
  }
}

include(
    ":app",
    ":router",
    ":mobius-migration",
    ":mobius-base",
    ":simple-platform",
    ":simple-visuals",
    ":lint"
)
