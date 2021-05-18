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
  implementation("io.grpc:grpc-core:1.36.0")

  implementation("com.auth0:java-jwt:3.14.0")
  implementation("com.auth0:jwks-rsa:0.17.0")
  implementation("com.google.guava:guava:30.1-jre")

  // Logging
  implementation("org.slf4j:slf4j-api:1.7.30")
  // End Logging

  testImplementation("org.junit.jupiter:junit-jupiter:5.7.0")
}
