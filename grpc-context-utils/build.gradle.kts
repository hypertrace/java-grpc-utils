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
  api(platform("io.grpc:grpc-bom:1.50.0"))
  implementation("io.grpc:grpc-core")

  implementation("com.auth0:java-jwt:4.4.0")
  implementation("com.auth0:jwks-rsa:0.22.0")
  implementation("com.google.guava:guava:31.1-jre")
  implementation("org.slf4j:slf4j-api:1.7.36")

  annotationProcessor("org.projectlombok:lombok:1.18.24")
  compileOnly("org.projectlombok:lombok:1.18.24")

  constraints {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.4.2") {
      because("https://nvd.nist.gov/vuln/detail/CVE-2022-42003")
    }
    implementation("com.google.protobuf:protobuf-java:3.21.7") {
      // Not used directly, but typically used together for since we always use proto and grpc together
      because("CVE-2022-3171")
    }
  }

  testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
  testImplementation("org.mockito:mockito-core:4.4.0")
  testImplementation("com.fasterxml.jackson.core:jackson-annotations:2.13.4")
  testAnnotationProcessor("org.projectlombok:lombok:1.18.24")
  testCompileOnly("org.projectlombok:lombok:1.18.24")
}
