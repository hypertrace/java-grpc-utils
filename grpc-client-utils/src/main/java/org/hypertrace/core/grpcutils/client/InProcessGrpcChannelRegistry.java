package org.hypertrace.core.grpcutils.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.inprocess.InProcessChannelBuilder;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class InProcessGrpcChannelRegistry extends GrpcChannelRegistry {
  private final Map<String, String> authorityToInProcessNamedOverride;

  public InProcessGrpcChannelRegistry() {
    this(Collections.emptyMap());
  }

  public InProcessGrpcChannelRegistry(Map<String, String> authorityToInProcessNamedOverride) {
    this.authorityToInProcessNamedOverride = authorityToInProcessNamedOverride;
  }

  public InProcessGrpcChannelRegistry(GrpcChannelRegistry sourceRegistry) {
    super(sourceRegistry);
    if (sourceRegistry instanceof InProcessGrpcChannelRegistry) {
      this.authorityToInProcessNamedOverride =
          Map.copyOf(
              ((InProcessGrpcChannelRegistry) sourceRegistry).authorityToInProcessNamedOverride);
    } else {
      this.authorityToInProcessNamedOverride = Collections.emptyMap();
    }
  }

  public ManagedChannel forName(String name) {
    return this.forName(name, GrpcChannelConfig.builder().build());
  }

  public ManagedChannel forName(String name, GrpcChannelConfig config) {
    assert !this.isShutdown();
    return this.getOrComputeChannel(
        this.getInProcessChannelId(name, config), () -> this.buildInProcessChannel(name, config));
  }

  String getChannelId(String host, int port, boolean isPlaintext, GrpcChannelConfig config) {
    return this.getInProcessOverrideNameForHostAndPort(host, port)
        .map(name -> this.getInProcessChannelId(name, config))
        .orElseGet(() -> super.getChannelId(host, port, isPlaintext, config));
  }

  @Override
  ManagedChannelBuilder<?> getBuilderForAddress(String host, int port) {
    return this.getInProcessOverrideNameForHostAndPort(host, port)
        .<ManagedChannelBuilder<?>>map(InProcessChannelBuilder::forName)
        .orElseGet(() -> super.getBuilderForAddress(host, port));
  }

  ManagedChannel buildInProcessChannel(String name, GrpcChannelConfig config) {
    return this.configureAndBuildChannel(InProcessChannelBuilder.forName(name), config);
  }

  String getInProcessChannelId(String name, GrpcChannelConfig config) {
    return "inprocess:" + name + ":" + Objects.hash(config);
  }

  private Optional<String> getInProcessOverrideNameForHostAndPort(String host, int port) {
    return Optional.ofNullable(
        this.authorityToInProcessNamedOverride.get(this.toAuthority(host, port)));
  }

  private String toAuthority(String host, int port) {
    return host + ":" + port;
  }
}
