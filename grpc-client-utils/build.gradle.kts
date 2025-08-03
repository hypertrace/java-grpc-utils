plugins {
  `java-library`
  jacoco
  alias(commonLibs.plugins.hypertrace.publish)
  alias(commonLibs.plugins.hypertrace.jacoco)
}

dependencies {

  api(commonLibs.grpc.context)
  api(commonLibs.grpc.api)
  api(commonLibs.grpc.inprocess)

  implementation(projects.grpcContextUtils)
  implementation(commonLibs.slf4j2.api)
  implementation(commonLibs.grpc.core)

  annotationProcessor(commonLibs.lombok)
  compileOnly(commonLibs.lombok)

  testImplementation(commonLibs.junit.jupiter)
  testImplementation(commonLibs.mockito.core)
  testRuntimeOnly(commonLibs.grpc.netty)
}

tasks.test {
  useJUnitPlatform()
}
