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
  api("io.grpc:grpc-context")
  api("io.grpc:grpc-api")

  api(platform("io.netty:netty-bom:4.1.79.Final")) {
    because("https://snyk.io/vuln/SNYK-JAVA-IONETTY-2812456")
  }

  implementation(project(":grpc-context-utils"))
  implementation("org.slf4j:slf4j-api:1.7.36")

  annotationProcessor("org.projectlombok:lombok:1.18.24")
  compileOnly("org.projectlombok:lombok:1.18.24")

  testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
  testImplementation("org.mockito:mockito-core:4.4.0")
}
