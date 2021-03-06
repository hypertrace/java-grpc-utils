plugins {
  `java-library`
  jacoco
  id("org.hypertrace.publish-plugin")
  id("org.hypertrace.jacoco-report-plugin")
}

dependencies {
  api("io.reactivex.rxjava3:rxjava:3.0.6")
  api("io.grpc:grpc-stub:1.36.0")

  annotationProcessor("org.projectlombok:lombok:1.18.18")
  compileOnly("org.projectlombok:lombok:1.18.18")

  implementation("org.slf4j:slf4j-api:1.7.30")
  
  constraints {
    implementation("com.google.guava:guava:30.1-jre") {
      because("https://snyk.io/vuln/SNYK-JAVA-COMGOOGLEGUAVA-1015415")
    }
  }
  testImplementation("org.junit.jupiter:junit-jupiter:5.7.0")
  testImplementation("org.mockito:mockito-core:3.5.11")
  testImplementation("org.mockito:mockito-junit-jupiter:3.5.11")
}

tasks.test {
  useJUnitPlatform()
}