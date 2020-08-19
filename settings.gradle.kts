rootProject.name = "grpc-utils"

pluginManagement {
  repositories {
    mavenLocal()
    gradlePluginPortal()
    maven("https://dl.bintray.com/hypertrace/maven")
  }
}

plugins {
  id("org.hypertrace.version-settings") version "0.1.1"
}

include(":grpc-client-utils")
include(":grpc-client-rx-utils")
include(":grpc-context-utils")
include(":grpc-server-utils")
