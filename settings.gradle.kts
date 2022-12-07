rootProject.name = "grpc-utils"

pluginManagement {
  repositories {
    mavenLocal()
    gradlePluginPortal()
    maven("https://hypertrace.jfrog.io/artifactory/maven")
  }
}

plugins {
  id("org.hypertrace.version-settings") version "0.2.0"
}

include(":grpc-client-utils")
include(":grpc-client-rx-utils")
include(":grpc-server-rx-utils")
include(":grpc-context-utils")
include(":grpc-server-utils")
include(":grpc-validation-utils")
