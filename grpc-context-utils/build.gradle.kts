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
  implementation("io.grpc:grpc-core:1.42.0")

  implementation("com.auth0:java-jwt:3.14.0")
  implementation("com.auth0:jwks-rsa:0.17.0")
  implementation("com.google.guava:guava:30.1-jre")

  // Logging
  implementation("org.slf4j:slf4j-api:1.7.30")
  // End Logging

  constraints {
    implementation("com.google.code.gson:gson:2.8.9") {
      because("https://snyk.io/vuln/SNYK-JAVA-COMGOOGLECODEGSON-1730327")
    }
  }

  testImplementation("org.junit.jupiter:junit-jupiter:5.7.0")
  testImplementation("org.mockito:mockito-core:3.12.1")
}
