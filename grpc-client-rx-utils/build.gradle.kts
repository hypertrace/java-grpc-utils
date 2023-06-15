plugins {
  `java-library`
  jacoco
  id("org.hypertrace.publish-plugin")
  id("org.hypertrace.jacoco-report-plugin")
}

dependencies {
  api(platform("io.grpc:grpc-bom:1.56.0"))
  api("io.reactivex.rxjava3:rxjava:3.1.4")
  api("io.grpc:grpc-stub")
  api(project(":grpc-context-utils"))
  implementation("io.grpc:grpc-context")

  testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
  testImplementation("org.mockito:mockito-core:4.4.0")
  testImplementation("org.mockito:mockito-junit-jupiter:4.4.0")
}

tasks.test {
  useJUnitPlatform()
}
