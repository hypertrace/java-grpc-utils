plugins {
  `java-library`
  jacoco
  id("org.hypertrace.publish-plugin")
  id("org.hypertrace.jacoco-report-plugin")
}

dependencies {
  api(platform("io.grpc:grpc-bom:1.43.1"))
  api("io.reactivex.rxjava3:rxjava:3.1.3")
  api("io.grpc:grpc-stub")

  annotationProcessor("org.projectlombok:lombok:1.18.22")
  compileOnly("org.projectlombok:lombok:1.18.22")

  implementation("org.slf4j:slf4j-api:1.7.32")

  testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
  testImplementation("org.mockito:mockito-core:4.2.0")
  testImplementation("org.mockito:mockito-junit-jupiter:4.2.0")
}

tasks.test {
  useJUnitPlatform()
}