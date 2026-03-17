plugins {
  `java-library`
  jacoco
  alias(commonLibs.plugins.hypertrace.publish)
  alias(commonLibs.plugins.hypertrace.jacoco)
}

tasks.test {
  useJUnitPlatform()
}

dependencies {
  api(commonLibs.grpc.context)
  api(commonLibs.grpc.api)
  implementation(projects.grpcContextUtils)
  implementation(commonLibs.slf4j2.api)
  compileOnly("org.apache.logging.log4j:log4j-core:2.20.0")

  annotationProcessor(commonLibs.lombok)
  compileOnly(commonLibs.lombok)

  testImplementation(commonLibs.junit.jupiter)
  testImplementation(commonLibs.mockito.core)
  testImplementation(commonLibs.mockito.junit)
}
