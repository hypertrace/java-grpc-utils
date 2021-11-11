plugins {
  `java-library`
  jacoco
  id("org.hypertrace.publish-plugin")
  id("org.hypertrace.jacoco-report-plugin")
}

dependencies {
  api("io.reactivex.rxjava3:rxjava:3.0.6")
  api("io.grpc:grpc-stub:1.42.0")
  api(project(":grpc-context-utils"))
  implementation("io.grpc:grpc-context:1.42.0")

  constraints {
    implementation("com.google.code.gson:gson:2.8.9") {
      because("https://snyk.io/vuln/SNYK-JAVA-COMGOOGLECODEGSON-1730327")
    }
  }
  
  testImplementation("org.junit.jupiter:junit-jupiter:5.7.0")
  testImplementation("org.mockito:mockito-core:3.12.1")
  testImplementation("org.mockito:mockito-junit-jupiter:3.12.1")
}

tasks.test {
  useJUnitPlatform()
}
