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
  api("io.grpc:grpc-context:1.42.0")
  api("io.grpc:grpc-api:1.42.0")

  implementation(project(":grpc-context-utils"))
  implementation("org.slf4j:slf4j-api:1.7.30")

  annotationProcessor("org.projectlombok:lombok:1.18.20")
  compileOnly("org.projectlombok:lombok:1.18.20")

  testImplementation("org.junit.jupiter:junit-jupiter:5.7.0")
  testImplementation("org.mockito:mockito-core:3.12.1")
}
