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
  api(platform("io.grpc:grpc-bom:1.43.1"))
  implementation("io.grpc:grpc-core")

  implementation("com.auth0:java-jwt:3.18.2")
  implementation("com.auth0:jwks-rsa:0.20.0")
  implementation("com.google.guava:guava:31.0.1-jre")
  implementation("org.slf4j:slf4j-api:1.7.32")

  constraints {
    api("com.fasterxml.jackson.core:jackson-databind:2.13.1") {
      because("https://snyk.io/vuln/SNYK-JAVA-COMFASTERXMLJACKSONCORE-232")
    }
    api("io.netty:netty-codec-http2:4.1.68.Final") {
      because("Multiple vulnerabilities")
    }
    api("io.netty:netty-handler-proxy:4.1.71.Final"){
      because("Multiple vulnerabilities")
    }
    api("com.google.code.gson:gson:2.8.9"){
      because("https://snyk.io/vuln/SNYK-JAVA-COMGOOGLECODEGSON-1730327")
    }
  }

  testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
  testImplementation("org.mockito:mockito-core:4.2.0")
}
