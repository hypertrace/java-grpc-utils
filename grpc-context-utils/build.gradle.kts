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
  implementation(commonLibs.grpc.core)

  implementation(localLibs.auth0.jwt)
  implementation(localLibs.auth0.jwks.rsa)
  implementation(commonLibs.guava)
  implementation(commonLibs.slf4j2.api)

  annotationProcessor(commonLibs.lombok)
  compileOnly(commonLibs.lombok)

  testImplementation(commonLibs.junit.jupiter)
  testImplementation(commonLibs.mockito.core)
  testImplementation(commonLibs.mockito.junit)
  testImplementation(commonLibs.jackson.databind)
  testAnnotationProcessor(commonLibs.lombok)
  testCompileOnly(commonLibs.lombok)
}
