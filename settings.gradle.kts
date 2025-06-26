rootProject.name = "grpc-utils"

pluginManagement {
  repositories {
    mavenLocal()
    gradlePluginPortal()
    maven("https://us-maven.pkg.dev/hypertrace-repos/maven")
  }
}

plugins {
  id("org.hypertrace.version-settings") version "0.3.0"
}

include(":grpc-client-utils")
include(":grpc-client-rx-utils")
include(":grpc-server-rx-utils")
include(":grpc-context-utils")
include(":grpc-server-utils")
include(":grpc-validation-utils")
include(":grpc-circuitbreaker-utils")
