plugins {
  `java-library`
  jacoco
  alias(commonLibs.plugins.hypertrace.publish)
  alias(commonLibs.plugins.hypertrace.jacoco)
}

dependencies {
  api(commonLibs.rxjava3)
  api(commonLibs.grpc.stub)
  api(projects.grpcContextUtils)
  implementation(commonLibs.grpc.context)

  testImplementation(commonLibs.junit.jupiter)
  testImplementation(commonLibs.mockito.core)
  testImplementation(commonLibs.mockito.junit)
}

tasks.test {
  useJUnitPlatform()
}
