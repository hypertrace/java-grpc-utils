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
  api("io.grpc:grpc-context")
  api("io.grpc:grpc-api")
  constraints {
    api("com.google.protobuf:protobuf-java:3.25.5") {
      because("https://nvd.nist.gov/vuln/detail/CVE-2024-7254")
    }
  }

  implementation(project(":grpc-context-utils"))
  implementation("org.slf4j:slf4j-api:1.7.36")

  annotationProcessor("org.projectlombok:lombok:1.18.24")
  compileOnly("org.projectlombok:lombok:1.18.24")

  testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
  testImplementation("org.mockito:mockito-core:5.8.0")
  testImplementation("org.mockito:mockito-junit-jupiter:5.8.0")
}
