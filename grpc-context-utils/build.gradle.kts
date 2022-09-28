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
  api(platform("io.grpc:grpc-bom:1.45.1"))
  implementation("io.grpc:grpc-core")

  implementation("com.auth0:java-jwt:3.19.1")
  implementation("com.auth0:jwks-rsa:0.21.1")
  implementation("com.google.guava:guava:31.1-jre")
  implementation("org.slf4j:slf4j-api:1.7.36")

  testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
  testImplementation("org.mockito:mockito-core:4.4.0")
  testImplementation("com.fasterxml.jackson.core:jackson-annotations:2.13.4")
  testAnnotationProcessor("org.projectlombok:lombok:1.18.24")
  testCompileOnly("org.projectlombok:lombok:1.18.24")
}
