plugins {
  `java-library`
  alias(commonLibs.plugins.hypertrace.publish)
}

dependencies {
  api(projects.grpcContextUtils)
  api(commonLibs.grpc.api)
  implementation(commonLibs.protobuf.javautil)
}
