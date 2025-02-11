plugins {
  `java-library`
  jacoco
  id("org.hypertrace.publish-plugin")
  id("org.hypertrace.jacoco-report-plugin")
}

dependencies {
  api(platform("io.grpc:grpc-bom:1.68.3"))
  api("io.reactivex.rxjava3:rxjava:3.1.4")
  api("io.grpc:grpc-stub")
  api(project(":grpc-context-utils"))
  constraints {
    api("com.google.protobuf:protobuf-java:3.25.5") {
      because("https://nvd.nist.gov/vuln/detail/CVE-2024-7254")
    }
  }
  implementation("io.grpc:grpc-context")

  testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
  testImplementation("org.mockito:mockito-core:5.8.0")
  testImplementation("org.mockito:mockito-junit-jupiter:5.8.0")
}

tasks.test {
  useJUnitPlatform()
}
