plugins {
  `java-library`
  jacoco
  id("org.hypertrace.publish-plugin")
  id("org.hypertrace.jacoco-report-plugin")
}

dependencies {
  api("io.grpc:grpc-context:1.40.0")
  api("io.grpc:grpc-api:1.40.0")

  implementation(project(":grpc-context-utils"))
  implementation("org.slf4j:slf4j-api:1.7.30")

  annotationProcessor("org.projectlombok:lombok:1.18.18")
  compileOnly("org.projectlombok:lombok:1.18.18")

  constraints {
    implementation("com.google.code.gson:gson:2.8.9") {
      because("https://snyk.io/vuln/SNYK-JAVA-COMGOOGLECODEGSON-1730327")
    }
  }

  testImplementation("org.junit.jupiter:junit-jupiter:5.7.0")
  testImplementation("org.mockito:mockito-core:3.12.1")
  testImplementation("org.mockito:mockito-inline:3.12.1")
  testRuntimeOnly("io.grpc:grpc-netty:1.40.0")
}

tasks.test {
  useJUnitPlatform()
}
