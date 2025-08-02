plugins {
  `java-library`
  jacoco
  alias(commonLibs.plugins.hypertrace.publish)
  alias(commonLibs.plugins.hypertrace.jacoco)
}

dependencies {
  api(commonLibs.grpc.api)
  api(projects.grpcContextUtils)

  implementation(commonLibs.slf4j2.api)
  implementation(localLibs.resilience4j.circuitbreaker)
  implementation(commonLibs.typesafe.config)
  implementation(commonLibs.guava)
  implementation(localLibs.jakarta.inject.api)

  annotationProcessor(commonLibs.lombok)
  compileOnly(commonLibs.lombok)

  testImplementation(commonLibs.junit.jupiter)
  testImplementation(commonLibs.mockito.core)
  testImplementation(commonLibs.mockito.junit)
}

tasks.test {
  useJUnitPlatform()
}
