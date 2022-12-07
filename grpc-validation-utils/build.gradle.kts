plugins {
  `java-library`
  id("org.hypertrace.publish-plugin")
}

dependencies {
  api(project(":grpc-context-utils"))
  api("io.grpc:grpc-api")
  implementation("com.google.protobuf:protobuf-java-util:3.21.7")
}
