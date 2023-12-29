plugins {
  `java-library`
  jacoco
  id("org.hypertrace.publish-plugin")
  id("org.hypertrace.jacoco-report-plugin")
}

dependencies {

  api(platform("io.grpc:grpc-bom:1.60.0"))
  api("io.grpc:grpc-context")
  api("io.grpc:grpc-api")
  api("io.grpc:grpc-inprocess")
  api(platform("io.netty:netty-bom:4.1.101.Final")) {
    because("CVE-2023-44487")
  }

  implementation(project(":grpc-context-utils"))
  implementation("org.slf4j:slf4j-api:1.7.36")
  implementation("io.grpc:grpc-core")

  annotationProcessor("org.projectlombok:lombok:1.18.24")
  compileOnly("org.projectlombok:lombok:1.18.24")

  testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
  testImplementation("org.mockito:mockito-core:5.8.0")
  testRuntimeOnly("io.grpc:grpc-netty")
}

tasks.test {
  useJUnitPlatform()
}
