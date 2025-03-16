plugins {
  `java-library`
  jacoco
  id("org.hypertrace.publish-plugin")
  id("org.hypertrace.jacoco-report-plugin")
}

dependencies {

  api(platform("io.grpc:grpc-bom:1.68.3"))
  api("io.grpc:grpc-api")
  api(project(":grpc-context-utils"))

  implementation("org.slf4j:slf4j-api:1.7.36")
  implementation("io.github.resilience4j:resilience4j-circuitbreaker:1.7.1")
  implementation("com.typesafe:config:1.4.2")
  implementation("com.google.inject:guice:7.0.0")

  annotationProcessor("org.projectlombok:lombok:1.18.24")
  compileOnly("org.projectlombok:lombok:1.18.24")

  testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
  testImplementation("org.mockito:mockito-core:5.8.0")
  testImplementation("org.mockito:mockito-junit-jupiter:5.8.0")
}

tasks.test {
  useJUnitPlatform()
}
