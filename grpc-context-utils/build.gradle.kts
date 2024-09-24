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
  api(platform("io.grpc:grpc-bom:1.60.0"))
  api(platform("com.fasterxml.jackson:jackson-bom:2.16.0"))
  constraints {
    api("com.google.protobuf:protobuf-java:3.25.5") {
      because("https://nvd.nist.gov/vuln/detail/CVE-2024-7254")
    }
  }
  implementation("io.grpc:grpc-core")

  implementation("com.auth0:java-jwt:4.4.0")
  implementation("com.auth0:jwks-rsa:0.22.0")
  implementation("com.google.guava:guava:32.0.1-jre")
  implementation("org.slf4j:slf4j-api:2.0.7")

  annotationProcessor("org.projectlombok:lombok:1.18.24")
  compileOnly("org.projectlombok:lombok:1.18.24")

  testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
  testImplementation("org.mockito:mockito-core:5.8.0")
  testImplementation("org.mockito:mockito-junit-jupiter:5.8.0")
  testImplementation("com.fasterxml.jackson.core:jackson-annotations:2.15.2")
  testAnnotationProcessor("org.projectlombok:lombok:1.18.24")
  testCompileOnly("org.projectlombok:lombok:1.18.24")
}
