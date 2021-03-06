plugins {
  `java-library`
  jacoco
  id("org.hypertrace.publish-plugin")
  id("org.hypertrace.jacoco-report-plugin")
}

dependencies {
  api("io.reactivex.rxjava3:rxjava:3.0.6")
  api("io.grpc:grpc-stub:1.36.0")
  api(project(":grpc-context-utils"))
  implementation("io.grpc:grpc-context:1.36.0")

  testImplementation("org.junit.jupiter:junit-jupiter:5.7.0")
  testImplementation("org.mockito:mockito-core:3.5.11")
  testImplementation("org.mockito:mockito-junit-jupiter:3.5.11")
}

tasks.test {
  useJUnitPlatform()
}