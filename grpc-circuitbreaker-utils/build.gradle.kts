plugins {
  `java-library`
  jacoco
  id("org.hypertrace.publish-plugin")
  id("org.hypertrace.jacoco-report-plugin")
}

dependencies {

  api(platform("io.grpc:grpc-bom:1.68.3"))
  api("io.grpc:grpc-context")
  api("io.grpc:grpc-api")
  api("io.grpc:grpc-inprocess")
  api(platform("io.netty:netty-bom:4.1.118.Final"))
  constraints {
    api("com.google.protobuf:protobuf-java:3.25.5") {
      because("https://nvd.nist.gov/vuln/detail/CVE-2024-7254")
    }
  }

  implementation(project(":grpc-context-utils"))
  implementation("org.slf4j:slf4j-api:1.7.36")
  implementation("io.grpc:grpc-core")
  implementation("io.github.resilience4j:resilience4j-circuitbreaker:1.7.1")
  implementation("com.typesafe:config:1.4.2")
  implementation("com.google.inject:guice:7.0.0")
  implementation("org.hypertrace.core.serviceframework:platform-metrics:0.1.87")

  annotationProcessor("org.projectlombok:lombok:1.18.24")
  compileOnly("org.projectlombok:lombok:1.18.24")

  testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
  testImplementation("org.mockito:mockito-core:5.8.0")
  testRuntimeOnly("io.grpc:grpc-netty")
}

tasks.test {
  useJUnitPlatform()
}
