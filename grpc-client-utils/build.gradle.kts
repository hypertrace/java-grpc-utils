plugins {
  `java-library`
  jacoco
  id("org.hypertrace.publish-plugin")
  id("org.hypertrace.jacoco-report-plugin")
}

dependencies {
  implementation(project(":grpc-context-utils"))

  // Logging
  implementation("org.slf4j:slf4j-api:1.7.30")
  // End Logging

  // grpc
  implementation("io.grpc:grpc-core:1.32.1")

  testImplementation("org.junit.jupiter:junit-jupiter:5.7.0")
  testImplementation("org.mockito:mockito-core:3.4.4")
}

tasks.test {
  useJUnitPlatform()
}