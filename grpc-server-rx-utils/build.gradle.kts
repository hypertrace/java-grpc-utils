plugins {
  `java-library`
  jacoco
  id("org.hypertrace.publish-plugin")
  id("org.hypertrace.jacoco-report-plugin")
}

dependencies {
  api(platform("io.grpc:grpc-bom:1.59.1"))
  api("io.reactivex.rxjava3:rxjava:3.1.4")
  api("io.grpc:grpc-stub")

  annotationProcessor("org.projectlombok:lombok:1.18.24")
  compileOnly("org.projectlombok:lombok:1.18.24")

  implementation("org.slf4j:slf4j-api:1.7.36")

  testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
  testImplementation("org.mockito:mockito-core:4.4.0")
  testImplementation("org.mockito:mockito-junit-jupiter:4.4.0")
}

tasks.test {
  useJUnitPlatform()
}
