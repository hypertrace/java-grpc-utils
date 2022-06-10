plugins {
  `java-library`
  jacoco
  id("org.hypertrace.publish-plugin")
  id("org.hypertrace.jacoco-report-plugin")
}

dependencies {
  constraints {
    api("io.netty:netty-codec-http2:4.1.77.Final") {
      because("https://snyk.io/vuln/SNYK-JAVA-IONETTY-2812456")
    }
  }
  api(platform("io.grpc:grpc-bom:1.45.1"))
  api("io.grpc:grpc-context")
  api("io.grpc:grpc-api")

  implementation(project(":grpc-context-utils"))
  implementation("org.slf4j:slf4j-api:1.7.36")

  annotationProcessor("org.projectlombok:lombok:1.18.22")
  compileOnly("org.projectlombok:lombok:1.18.22")

  testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
  testImplementation("org.mockito:mockito-core:4.4.0")
  testImplementation("org.mockito:mockito-inline:4.4.0")
  testRuntimeOnly("io.grpc:grpc-netty")
}

tasks.test {
  useJUnitPlatform()
}
