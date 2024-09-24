import org.hypertrace.gradle.publishing.HypertracePublishExtension
import org.hypertrace.gradle.publishing.License

plugins {
  id("org.hypertrace.repository-plugin") version "0.4.0"
  id("org.hypertrace.ci-utils-plugin") version "0.3.2"
  id("org.hypertrace.publish-plugin") version "1.0.5" apply false
  id("org.hypertrace.jacoco-report-plugin") version "0.2.1" apply false
  id("org.hypertrace.code-style-plugin") version "2.0.0" apply false
  id("org.owasp.dependencycheck") version "10.0.3"
}

subprojects {
  group = "org.hypertrace.core.grpcutils"
  pluginManager.withPlugin("org.hypertrace.publish-plugin") {
    configure<HypertracePublishExtension> {
      license.set(License.APACHE_2_0)
    }
  }

  pluginManager.withPlugin("java") {
    configure<JavaPluginExtension> {
      sourceCompatibility = JavaVersion.VERSION_11
      targetCompatibility = JavaVersion.VERSION_11
    }
  }

  apply(plugin = "org.hypertrace.code-style-plugin")
}

dependencyCheck {
  format = org.owasp.dependencycheck.reporting.ReportGenerator.Format.ALL.toString()
  suppressionFile = "owasp-suppressions.xml"
  scanConfigurations.add("runtimeClasspath")
  failBuildOnCVSS = 3.0F
}