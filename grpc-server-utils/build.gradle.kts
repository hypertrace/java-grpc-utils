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
  implementation(project(":grpc-context-utils"))

  // Logging
  implementation("org.slf4j:slf4j-api:1.7.30")
  // End Logging

  // grpc
  implementation("io.grpc:grpc-core:1.40.0")
  constraints {
    implementation("com.google.guava:guava:30.0-jre") {
      because("https://snyk.io/vuln/SNYK-JAVA-COMGOOGLEGUAVA-1015415")
    }
  }

  annotationProcessor("org.projectlombok:lombok:1.18.20")
  compileOnly("org.projectlombok:lombok:1.18.20")

  testImplementation("org.junit.jupiter:junit-jupiter:5.7.0")
  testImplementation("org.mockito:mockito-core:3.12.1")
}
