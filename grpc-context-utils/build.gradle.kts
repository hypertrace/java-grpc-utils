plugins {
  `java-library`
  jacoco
  id("org.hypertrace.publish-plugin")
  id("org.hypertrace.jacoco-report-plugin")
}

tasks.test {
  useJUnitPlatform()
}

dependencies {
  // grpc
  implementation("io.grpc:grpc-core:1.31.0")

  // Logging
  implementation("org.slf4j:slf4j-api:1.7.30")
  // End Logging

  testImplementation("org.junit.jupiter:junit-jupiter:5.6.2")
}
